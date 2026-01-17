package com.quest.voiceover.features;

import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.modules.audio.AudioManager;
import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.dialog.DialogManager;
import com.quest.voiceover.utility.MessageUtility;
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

    private static final double LEVENSHTEIN_THRESHOLD = 0.70;

    private static final String EXACT_QUERY =
        "SELECT quest, uri, text FROM dialogs WHERE character = ? AND text = ? LIMIT 1";

    private static final String LEVENSHTEIN_QUERY =
        "SELECT quest, uri, text, levenshtein_similarity(text, ?) AS similarity " +
        "FROM dialogs WHERE character = ? " +
        "ORDER BY similarity DESC LIMIT 1";

    @Inject
    private ClientThread clientThread;

    @Inject
    private DatabaseManager databaseManager;

    @Inject
    private AudioManager audioManager;

    @Inject
    private DialogManager dialogManager;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private DialogSpeechHighlightHandler dialogSpeechHighlightHandler;

    @Getter
    private boolean activeVoiceover;

    @Getter
    private String currentQuestName;

    private String pendingCharacter;
    private String pendingPlayerName;
    private String pendingOriginalText;

    /**
     * Chat messages serve as a reliable trigger event, while the dialog widget provides
     * the complete text. Widget-only detection is unreliable because there's no single
     * "dialog changed" event and widget population timing is unpredictable. The chat
     * message signals "dialog happened", then we fetch the full content from the widget.
     */
    public void handleDialogMessage(String rawMessage, String playerName) {
        String playerVoiceName = config.playerVoice().getCharacterName();
        MessageUtility.ParsedMessage chatMessage = MessageUtility.parseRawMessage(rawMessage, playerName, playerVoiceName);
        String chatText = chatMessage.dialogText();
        String chatCharacter = chatMessage.characterName();

        String widgetText = dialogManager.getDialogText();
        String widgetCharacter = dialogManager.getDialogCharacterName();

        if (widgetText == null || widgetCharacter == null) {
            log.debug("Widget not available, scheduling retry");
            scheduleRetry(chatCharacter, playerName, null);
            return;
        }

        String cleanedWidgetText = MessageUtility.cleanWidgetText(widgetText, playerName);
        boolean textMatches = cleanedWidgetText.startsWith(chatText) || chatText.startsWith(cleanedWidgetText);

        if (!textMatches) {
            log.debug("Widget text mismatch, scheduling retry");
            scheduleRetry(chatCharacter, playerName, widgetText);
            return;
        }

        playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText, widgetText);
    }

    private void scheduleRetry(String character, String playerName, String originalText) {
        pendingCharacter = character;
        pendingPlayerName = playerName;
        pendingOriginalText = originalText;
        clientThread.invokeLater(this::retryWithWidget);
    }

    private void retryWithWidget() {
        if (pendingCharacter == null) {
            return;
        }

        String widgetText = dialogManager.getDialogText();
        String widgetCharacter = dialogManager.getDialogCharacterName();

        if (widgetText == null || widgetCharacter == null) {
            log.debug("Widget retry failed");
            clearPendingState();
            return;
        }

        String cleanedWidgetText = MessageUtility.cleanWidgetText(widgetText, pendingPlayerName);
        playVoiceoverIfAvailable(widgetCharacter, cleanedWidgetText, widgetText);
        clearPendingState();
    }

    private void clearPendingState() {
        pendingCharacter = null;
        pendingPlayerName = null;
        pendingOriginalText = null;
    }

    public void handleDialogOpened() {
    }

    public void stopVoiceover() {
        activeVoiceover = false;
        audioManager.stop();
        dialogSpeechHighlightHandler.stop();
    }

    /**
     * Query stages (in order of speed/accuracy tradeoff):
     * 1. Exact match - fastest, handles most cases where wiki text matches game text
     * 2. Levenshtein similarity - handles word substitutions (e.g., "called" vs "named")
     *    where wiki transcript differs from actual in-game text
     */
    private void playVoiceoverIfAvailable(String characterName, String dialogText, String originalText) {
        if (tryExactQuery(characterName, dialogText, originalText)) {
            return;
        }

        if (tryLevenshteinQuery(characterName, dialogText, originalText)) {
            return;
        }

        log.info("No voiceover found for {} - '{}'", characterName, dialogText);
        activeVoiceover = false;
        dialogSpeechHighlightHandler.stop();
        audioManager.stopImmediately();
    }

    private boolean tryExactQuery(String characterName, String dialogText, String originalText) {
        try (PreparedStatement statement = databaseManager.prepareStatement(EXACT_QUERY)) {
            statement.setString(1, characterName);
            statement.setString(2, dialogText);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    log.debug("Match type: exact");
                    return playVoiceoverFromResult(resultSet, characterName, dialogText, originalText);
                }
            }
        } catch (SQLException e) {
            log.error("Database query failed (exact)", e);
        }
        return false;
    }

    private boolean tryLevenshteinQuery(String characterName, String dialogText, String originalText) {
        try (PreparedStatement statement = databaseManager.prepareStatement(LEVENSHTEIN_QUERY)) {
            statement.setString(1, dialogText);
            statement.setString(2, characterName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }

                double similarity = resultSet.getDouble("similarity");
                if (similarity < LEVENSHTEIN_THRESHOLD) {
                    String bestMatchText = resultSet.getString("text");
                    log.info("Levenshtein match below threshold ({}%) for {} - '{}' best match: '{}'",
                        String.format("%.1f", similarity * 100), characterName, dialogText, bestMatchText);
                    return false;
                }

                log.debug("Match type: levenshtein ({}%)", String.format("%.1f", similarity * 100));
                return playVoiceoverFromResult(resultSet, characterName, dialogText, originalText);
            }
        } catch (SQLException e) {
            log.error("Database query failed (Levenshtein)", e);
        }
        return false;
    }

    private boolean playVoiceoverFromResult(ResultSet resultSet, String characterName, String dialogText, String originalText) throws SQLException {
        String audioUri = resultSet.getString("uri");
        currentQuestName = resultSet.getString("quest");
        String matchedText = resultSet.getString("text");
        log.info("Playing voiceover: {} - {} - '{}' matched: '{}'", characterName, currentQuestName, dialogText, matchedText);

        if (audioUri == null && currentQuestName == null) {
            return false;
        }

        activeVoiceover = true;
        audioManager.play(audioUri, currentQuestName, characterName);
        dialogSpeechHighlightHandler.startAsync(audioUri, originalText);
        return true;
    }

}
