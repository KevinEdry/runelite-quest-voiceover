package com.quest.voiceover.features;

import com.quest.voiceover.modules.audio.SoundEngine;
import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.dialog.DialogManager;
import com.quest.voiceover.utility.MessageParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Singleton
public class VoiceoverHandler {

    private static final String VOICEOVER_QUERY =
        "SELECT quest, uri FROM dialogs WHERE character = ? AND text MATCH ?";

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

    public void handleDialogMessage(String rawMessage, String playerName) {
        MessageParser message = new MessageParser(rawMessage, playerName);
        playVoiceoverIfAvailable(message);
    }

    public void handleDialogOpened() {
        if (!dialogManager.isPlayerOrNpcDialogOpen() || !activeVoiceover) {
            return;
        }

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
        soundEngine.stop();
    }

    private void playVoiceoverIfAvailable(MessageParser message) {
        try (PreparedStatement statement = databaseManager.prepareStatement(VOICEOVER_QUERY)) {
            statement.setString(1, escapeQuotes(message.getCharacterName()));
            statement.setString(2, escapeQuotes(message.getDialogText()));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String audioUri = resultSet.getString("uri");
                    currentQuestName = resultSet.getString("quest");

                    if (audioUri != null || currentQuestName != null) {
                        activeVoiceover = true;
                        soundEngine.play(audioUri);
                        return;
                    }
                }
            }

            activeVoiceover = false;
        } catch (SQLException e) {
            log.error("Failed to query voiceover database", e);
        }
    }

    private String escapeQuotes(String input) {
        return input.replace("'", "''");
    }
}
