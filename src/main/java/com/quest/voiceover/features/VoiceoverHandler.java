package com.quest.voiceover.features;

import com.quest.voiceover.QuestVoiceoverConfig;
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

    private static final double LEVENSHTEIN_THRESHOLD = 0.85;

    private static final String EXACT_QUERY =
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text = ? LIMIT 1";

    private static final String FTS_QUERY =
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text MATCH ? " +
        "UNION ALL " +
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text LIKE ? " +
        "LIMIT 1";

    private static final String LEVENSHTEIN_QUERY =
        "SELECT quest, uri, levenshtein_similarity(text, ?) AS similarity " +
        "FROM dialogs WHERE character = ? AND similarity >= ? " +
        "ORDER BY similarity DESC LIMIT 1";

    @Inject
    private ClientThread clientThread;

    @Inject
    private DatabaseManager databaseManager;

    @Inject
    private SoundEngine soundEngine;

    @Inject
    private DialogManager dialogManager;

    @Inject
    private QuestVoiceoverConfig config;

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
                playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText);
            } else {
                log.debug("Widget text mismatch, scheduling retry");
                pendingCharacter = chatCharacter;
                pendingPlayerName = playerName;
                clientThread.invokeLater(this::retryWithWidget);
            }
        } else {
            log.debug("Widget not available, scheduling retry");
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
            playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText);
        } else {
            log.debug("Widget retry failed");
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

        if (config.showMuteButton()) {
            dialogManager.addMuteButton(dialogWidget);
        }

        if (config.showQuestName() && currentQuestName != null) {
            dialogManager.addQuestNameLabel(dialogWidget, currentQuestName);
        }
    }

    public void stopVoiceover() {
        activeVoiceover = false;
        soundEngine.stop();
    }

    /**
     * Query stages (in order of speed/accuracy tradeoff):
     * 1. Exact match - fastest, handles most cases where wiki text matches game text
     * 2. FTS + LIKE prefix - handles partial matches and minor punctuation differences
     * 3. Levenshtein similarity - handles word substitutions (e.g., "called" vs "named")
     *    where wiki transcript differs from actual in-game text
     */
    private void playVoiceoverIfAvailable(String characterName, String dialogText) {
        String escapedCharacter = escapeQuotes(characterName);
        String escapedText = escapeQuotes(dialogText);

        if (tryExactQuery(escapedCharacter, escapedText, characterName)) {
            return;
        }

        String prefixSearch = escapedText.substring(0, Math.min(50, escapedText.length())) + "%";
        if (tryFtsQuery(escapedCharacter, escapedText, prefixSearch, characterName)) {
            return;
        }

        if (tryLevenshteinQuery(escapedCharacter, escapedText, characterName)) {
            return;
        }

        log.debug("No voiceover found for {} - '{}'", characterName, dialogText);
        activeVoiceover = false;
    }

    private boolean tryExactQuery(String escapedCharacter, String escapedText, String characterName) {
        try (PreparedStatement statement = databaseManager.prepareStatement(EXACT_QUERY)) {
            statement.setString(1, escapedCharacter);
            statement.setString(2, escapedText);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    log.debug("Match type: exact");
                    return playVoiceoverFromResult(resultSet, characterName);
                }
            }
        } catch (SQLException e) {
            log.error("Database query failed (exact)", e);
        }
        return false;
    }

    private boolean tryFtsQuery(String escapedCharacter, String escapedText, String prefixSearch, String characterName) {
        try (PreparedStatement statement = databaseManager.prepareStatement(FTS_QUERY)) {
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
            log.error("Database query failed (FTS)", e);
        }
        return false;
    }

    private boolean tryLevenshteinQuery(String escapedCharacter, String escapedText, String characterName) {
        try (PreparedStatement statement = databaseManager.prepareStatement(LEVENSHTEIN_QUERY)) {
            statement.setString(1, escapedText);
            statement.setString(2, escapedCharacter);
            statement.setDouble(3, LEVENSHTEIN_THRESHOLD);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double similarity = resultSet.getDouble("similarity");
                    log.debug("Match type: levenshtein ({}%)", String.format("%.1f", similarity * 100));
                    return playVoiceoverFromResult(resultSet, characterName);
                }
            }
        } catch (SQLException e) {
            log.error("Database query failed (Levenshtein)", e);
        }
        return false;
    }

    private boolean playVoiceoverFromResult(ResultSet resultSet, String characterName) throws SQLException {
        String audioUri = resultSet.getString("uri");
        currentQuestName = resultSet.getString("quest");
        log.info("Playing voiceover: {} - {}", characterName, currentQuestName);

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
