package com.quest.voiceover;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;

public class DialogEngine {
    @Inject
    private Client client;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    ConfigManager configManager;

    @Inject
    SoundEngine soundEngine;

    @Getter @Setter
    private Boolean dialogOpened = false;

    private static final String TOGGLE_MUTE = "Toggle mute ";
    private static final String PLUGIN_GROUP = "quest.voiceover";

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        // Check if the loaded widget is the dialog widget
        if (event.getGroupId() == WidgetID.DIALOG_NPC_GROUP_ID || event.getGroupId() == WidgetID.DIALOG_PLAYER_GROUP_ID || event.getGroupId() == WidgetID.DIALOG_OPTION_GROUP_ID)
        {
            Widget widget = getPlayerOrNpcWidget();
            if (widget != null) {
                addMuteButton(widget);
            }
        }
    }

    boolean isDialogOpen()
    {
        return client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 0) != null ||
                client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 0) != null ||
                client.getWidget(WidgetID.DIALOG_OPTION_GROUP_ID, 0) != null;
    }

    private boolean isPlayerOrNpcDialogOpen()
    {
        return client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 0) != null ||
                client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 0) != null;
    }

    private void addMuteButton(Widget widget) {


        Widget muteButton = widget.createChild(-1, WidgetType.GRAPHIC);

        //1661 (32 x 32) also viable
        //1662 (32 x 32)
        int musicSprite = config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;
        muteButton.setSpriteId(musicSprite);
        muteButton.setOriginalWidth(20);
        muteButton.setOriginalHeight(20);
        muteButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
        muteButton.setOriginalX(10);
        muteButton.setOriginalY(10);
        muteButton.setHasListener(true);
        muteButton.setAction(1, TOGGLE_MUTE);
        muteButton.setOnOpListener((JavaScriptCallback) e -> this.toggleMute(muteButton));
        muteButton.revalidate();
    }

    private void toggleMute(Widget muteButton)
    {
        configManager.setConfiguration(PLUGIN_GROUP, "mute", !config.mute());
        soundEngine.setVolume(config.mute() ? 0 : config.volume());
        int musicSprite = config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;
        muteButton.setSpriteId(musicSprite);
        muteButton.revalidate();
    }

    public Widget getPlayerOrNpcWidget() {
        if(!isPlayerOrNpcDialogOpen()){
            return null;
        }
        Widget playerWidget = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 0);
        Widget npcWidget = client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 0);
        return playerWidget != null ? playerWidget : npcWidget;
    }
}
