package com.quest.voiceover.modules.dialog;

import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.modules.audio.SoundEngine;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.*;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DialogManager {

    private static final String TOGGLE_MUTE_ACTION = "Toggle mute ";
    private static final String PLUGIN_CONFIG_GROUP = "quest.voiceover";
    private static final int FONT_ID = 494;
    private static final int TEXT_COLOR_WHITE = 0xFFFFFF;

    @Inject
    private Client client;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SoundEngine soundEngine;

    public boolean isPlayerOrNpcDialogOpen() {
        return getPlayerDialogWidget() != null || getNpcDialogWidget() != null;
    }

    public Widget getActiveDialogWidget() {
        if (!isPlayerOrNpcDialogOpen()) {
            return null;
        }

        Widget playerWidget = getPlayerDialogWidget();
        return playerWidget != null ? playerWidget : getNpcDialogWidget();
    }

    public void addQuestNameLabel(Widget dialogWidget, String questName) {
        Widget label = dialogWidget.createChild(-1, WidgetType.TEXT);

        label.setText("Quest: " + questName);
        label.setFontId(FONT_ID);
        label.setTextColor(TEXT_COLOR_WHITE);
        label.setTextShadowed(true);
        label.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
        label.setOriginalX(10);
        label.setOriginalY(5);
        label.setOriginalHeight(20);
        label.setOriginalWidth(200);
        label.revalidate();
    }

    public void addMuteButton(Widget dialogWidget) {
        Widget muteButton = dialogWidget.createChild(-1, WidgetType.GRAPHIC);

        muteButton.setSpriteId(getMuteSpriteId());
        muteButton.setOriginalWidth(32);
        muteButton.setOriginalHeight(32);
        muteButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
        muteButton.setOriginalX(5);
        muteButton.setOriginalY(5);
        muteButton.setHasListener(true);
        muteButton.setAction(1, TOGGLE_MUTE_ACTION);
        muteButton.setOnOpListener((JavaScriptCallback) e -> toggleMute(muteButton));
        muteButton.revalidate();
    }

    private Widget getPlayerDialogWidget() {
        return client.getWidget(InterfaceID.DIALOG_PLAYER, 0);
    }

    private Widget getNpcDialogWidget() {
        return client.getWidget(InterfaceID.DIALOG_NPC, 0);
    }

    private void toggleMute(Widget muteButton) {
        configManager.setConfiguration(PLUGIN_CONFIG_GROUP, "mute", !config.mute());
        soundEngine.setVolume(config.mute() ? 0 : config.volume());
        muteButton.setSpriteId(getMuteSpriteId());
        muteButton.revalidate();
    }

    private int getMuteSpriteId() {
        return config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;
    }
}
