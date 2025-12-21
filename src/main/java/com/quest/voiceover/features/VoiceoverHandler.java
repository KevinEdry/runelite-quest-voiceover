package com.quest.voiceover.features;

import com.quest.voiceover.modules.audio.SoundEngine;
import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.dialog.DialogManager;
import com.quest.voiceover.utility.MessageParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Singleton
public class VoiceoverHandler {

    private static final String VOICEOVER_QUERY_WITH_QUEST =
        "SELECT quest, uri FROM dialogs WHERE quest = ? AND character = ? AND text MATCH ? " +
        "UNION ALL " +
        "SELECT quest, uri FROM dialogs WHERE quest = ? AND character = ? AND text LIKE ? " +
        "LIMIT 1";

    private static final String VOICEOVER_QUERY =
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text MATCH ? " +
        "UNION ALL " +
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text LIKE ? " +
        "LIMIT 1";

    @Inject
    private ClientThread clientThread;

    @Inject
    private DatabaseManager databaseManager;

    @Inject
    private SoundEngine soundEngine;

    @Inject
    private DialogManager dialogManager;

    @Getter
    private boolean activeVoiceover;

    @Getter
    private String currentQuestName;

    private String pendingCharacter;
    private String pendingPlayerName;

    public void handleDialogMessage(String rawMessage, String playerName) {
        MessageParser chatMessage = new MessageParser(rawMessage, playerName);
        String chatText = chatMessage.getDialogText();
        String chatCharacter = chatMessage.getCharacterName();

        String widgetText = dialogManager.getDialogText();
        String widgetCharacter = dialogManager.getDialogCharacterName(playerName);

        if (widgetText != null && widgetCharacter != null) {
            String cleanedWidgetText = cleanWidgetText(widgetText, playerName);

            if (cleanedWidgetText.startsWith(chatText) || chatText.startsWith(cleanedWidgetText)) {
                log.info("Using widget text (length: {}) - matches chat message", cleanedWidgetText.length());
                playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText);
            } else {
                log.info("Widget text doesn't match chat message, scheduling retry. Widget: '{}...', Chat: '{}...'",
                    cleanedWidgetText.substring(0, Math.min(30, cleanedWidgetText.length())),
                    chatText.substring(0, Math.min(30, chatText.length())));
                pendingCharacter = chatCharacter;
                pendingPlayerName = playerName;
                clientThread.invokeLater(this::retryWithWidget);
            }
        } else {
            log.info("Widget text not available, scheduling retry");
            pendingCharacter = chatCharacter;
            pendingPlayerName = playerName;
            clientThread.invokeLater(this::retryWithWidget);
        }
    }

    private void retryWithWidget() {
        if (pendingCharacter == null) {
            return;
        }

        String widgetText = dialogManager.getDialogText();
        String widgetCharacter = dialogManager.getDialogCharacterName(pendingPlayerName);

        if (widgetText != null && widgetCharacter != null) {
            String cleanedWidgetText = cleanWidgetText(widgetText, pendingPlayerName);
            log.info("Retry successful - using widget text (length: {})", cleanedWidgetText.length());
            playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText);
        } else {
            log.warn("Retry failed - widget still not available");
        }

        pendingCharacter = null;
        pendingPlayerName = null;
    }

    private String cleanWidgetText(String text, String playerName) {
        String cleaned = text
            .replaceAll("<br>", " ")
            .replaceAll("<col=[^>]*>", "")
            .replaceAll("</col>", "")
            .trim();

        if (playerName != null && !playerName.isEmpty()) {
            cleaned = cleaned.replace(playerName, "");
        }

        return cleaned;
    }

    public void handleDialogOpened() {
        if (!dialogManager.isPlayerOrNpcDialogOpen() || !activeVoiceover) {
            return;
        }
        addDialogOverlay();
    }

    private void addDialogOverlay() {
        Widget dialogWidget = dialogManager.getActiveDialogWidget();
        if (dialogWidget == null) {
            return;
        }

        dialogManager.addMuteButton(dialogWidget);

        if (currentQuestName != null) {
            dialogManager.addQuestNameLabel(dialogWidget, currentQuestName);
        }
    }

    public void stopVoiceover() {
        activeVoiceover = false;
        soundEngine.stop();
    }

    private void playVoiceoverIfAvailable(String characterName, String dialogText) {
        String escapedCharacter = escapeQuotes(characterName);
        String escapedText = escapeQuotes(dialogText);
        String prefixSearch = escapedText.substring(0, Math.min(50, escapedText.length())) + "%";
        log.info("Querying database - character: '{}', text: '{}', prefix: '{}', currentQuest: '{}'",
            escapedCharacter, escapedText, prefixSearch, currentQuestName);

        if (currentQuestName != null && tryQueryWithQuest(escapedCharacter, escapedText, prefixSearch, characterName)) {
            return;
        }

        if (tryQueryWithoutQuest(escapedCharacter, escapedText, prefixSearch, characterName)) {
            return;
        }

        log.info("No voiceover found for character '{}': {}", characterName, dialogText);
        activeVoiceover = false;
    }

    private boolean tryQueryWithQuest(String escapedCharacter, String escapedText, String prefixSearch, String characterName) {
        try (PreparedStatement statement = databaseManager.prepareStatement(VOICEOVER_QUERY_WITH_QUEST)) {
            statement.setString(1, currentQuestName);
            statement.setString(2, escapedCharacter);
            statement.setString(3, escapedText);
            statement.setString(4, currentQuestName);
            statement.setString(5, escapedCharacter);
            statement.setString(6, prefixSearch);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return playVoiceoverFromResult(resultSet, characterName);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query voiceover database with quest filter", e);
        }
        log.info("No match in current quest '{}', trying all quests", currentQuestName);
        return false;
    }

    private boolean tryQueryWithoutQuest(String escapedCharacter, String escapedText, String prefixSearch, String characterName) {
        try (PreparedStatement statement = databaseManager.prepareStatement(VOICEOVER_QUERY)) {
            statement.setString(1, escapedCharacter);
            statement.setString(2, escapedText);
            statement.setString(3, escapedCharacter);
            statement.setString(4, prefixSearch);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return playVoiceoverFromResult(resultSet, characterName);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query voiceover database", e);
        }
        return false;
    }

    private boolean playVoiceoverFromResult(ResultSet resultSet, String characterName) throws SQLException {
        String audioUri = resultSet.getString("uri");
        currentQuestName = resultSet.getString("quest");
        log.info("Found voiceover - quest: '{}', uri: '{}', character: '{}'",
            currentQuestName, audioUri, characterName);

        if (audioUri != null || currentQuestName != null) {
            activeVoiceover = true;
            soundEngine.play(audioUri);
            addDialogOverlay();
            return true;
        }
        return false;
    }

    private String escapeQuotes(String input) {
        return input.replace("'", "''");
    }
}
