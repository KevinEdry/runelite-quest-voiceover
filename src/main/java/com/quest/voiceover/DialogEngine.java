package com.quest.voiceover;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;

public class DialogEngine {
    @Inject
    private Client client;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SoundEngine soundEngine;

    @Getter @Setter
    private Boolean dialogOpened = false;

    private static final String TOGGLE_MUTE = "Toggle mute ";
    private static final String PLUGIN_GROUP = "quest.voiceover";

    public boolean isDialogOpen()
    {
        return client.getWidget(InterfaceID.DIALOG_NPC, 0) != null ||
                client.getWidget(InterfaceID.DIALOG_PLAYER, 0) != null ||
                client.getWidget(InterfaceID.DIALOG_OPTION, 0) != null;
    }

    public boolean isPlayerOrNpcDialogOpen()
    {
        return client.getWidget(InterfaceID.DIALOG_NPC, 0) != null ||
                client.getWidget(InterfaceID.DIALOG_PLAYER, 0) != null;
    }

    public void addMuteButton(Widget widget) {
        Widget muteButton = widget.createChild(-1, WidgetType.GRAPHIC);
        int musicSprite = config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;

        muteButton.setSpriteId(musicSprite);
        muteButton.setOriginalWidth(32);
        muteButton.setOriginalHeight(32);
        muteButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
        muteButton.setOriginalX(10);
        muteButton.setOriginalY(10);
        muteButton.setHasListener(true);
        muteButton.setAction(1, TOGGLE_MUTE);
        muteButton.setOnOpListener((JavaScriptCallback) e -> this.toggleMute(muteButton));
        muteButton.revalidate();
    }

    public Widget getPlayerOrNpcWidget() {
        if(!isPlayerOrNpcDialogOpen()){
            return null;
        }
        Widget playerWidget = client.getWidget(InterfaceID.DIALOG_PLAYER, 0);
        Widget npcWidget = client.getWidget(InterfaceID.DIALOG_NPC, 0);
        return playerWidget != null ? playerWidget : npcWidget;
    }

    private void toggleMute(Widget muteButton)
    {
        configManager.setConfiguration(PLUGIN_GROUP, "mute", !config.mute());
        soundEngine.setVolume(config.mute() ? 0 : config.volume());
        int musicSprite = config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;
        muteButton.setSpriteId(musicSprite);
        muteButton.revalidate();
    }

}
