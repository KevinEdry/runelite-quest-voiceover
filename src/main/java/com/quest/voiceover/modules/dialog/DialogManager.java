package com.quest.voiceover.modules.dialog;

import com.quest.voiceover.QuestVoiceoverConfig;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DialogManager {

    private static final int DIALOG_NPC_TEXT_CHILD = 6;
    private static final int DIALOG_NPC_NAME_CHILD = 4;
    private static final int DIALOG_PLAYER_TEXT_CHILD = 6;

    @Inject
    private Client client;

    @Inject
    private QuestVoiceoverConfig config;

    public boolean isPlayerOrNpcDialogOpen() {
        Widget playerWidget = getPlayerDialogWidget();
        if (playerWidget != null && !playerWidget.isHidden()) {
            return true;
        }

        Widget npcWidget = getNpcDialogWidget();
        return npcWidget != null && !npcWidget.isHidden();
    }

    public Widget getActiveDialogWidget() {
        if (!isPlayerOrNpcDialogOpen()) {
            return null;
        }

        Widget playerWidget = getPlayerDialogWidget();
        return playerWidget != null ? playerWidget : getNpcDialogWidget();
    }

    public String getDialogText() {
        Widget npcTextWidget = client.getWidget(InterfaceID.DIALOG_NPC, DIALOG_NPC_TEXT_CHILD);
        if (npcTextWidget != null) {
            return npcTextWidget.getText();
        }

        Widget playerTextWidget = client.getWidget(InterfaceID.DIALOG_PLAYER, DIALOG_PLAYER_TEXT_CHILD);
        if (playerTextWidget != null) {
            return playerTextWidget.getText();
        }

        return null;
    }

    public void setDialogText(String text) {
        try {
            Widget npcTextWidget = client.getWidget(InterfaceID.DIALOG_NPC, DIALOG_NPC_TEXT_CHILD);
            Widget playerTextWidget = client.getWidget(InterfaceID.DIALOG_PLAYER, DIALOG_PLAYER_TEXT_CHILD);

            boolean npcVisible = npcTextWidget != null && !npcTextWidget.isHidden();
            boolean playerVisible = playerTextWidget != null && !playerTextWidget.isHidden();

            if (npcVisible) {
                npcTextWidget.setText(text);
                return;
            }

            if (playerVisible) {
                playerTextWidget.setText(text);
            }
        } catch (NullPointerException ignored) {
        }
    }

    public String getDialogCharacterName() {
        Widget npcNameWidget = client.getWidget(InterfaceID.DIALOG_NPC, DIALOG_NPC_NAME_CHILD);
        if (npcNameWidget != null) {
            return npcNameWidget.getText();
        }

        Widget playerTextWidget = client.getWidget(InterfaceID.DIALOG_PLAYER, DIALOG_PLAYER_TEXT_CHILD);
        if (playerTextWidget != null) {
            return config.playerVoice().getCharacterName(client);
        }

        return null;
    }

    private Widget getPlayerDialogWidget() {
        return client.getWidget(InterfaceID.DIALOG_PLAYER, 0);
    }

    private Widget getNpcDialogWidget() {
        return client.getWidget(InterfaceID.DIALOG_NPC, 0);
    }
}
