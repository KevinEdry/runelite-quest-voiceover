package com.quest.voiceover;

import com.google.inject.Provides;
import com.quest.voiceover.features.questlist.QuestListIndicatorManager;
import com.quest.voiceover.features.voiceover.VoiceoverHandler;
import com.quest.voiceover.modules.audio.SoundEngine;
import com.quest.voiceover.modules.database.DatabaseManager;
import com.quest.voiceover.modules.database.DatabaseVersionManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(name = "Quest Voiceover")
public class QuestVoiceoverPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private DatabaseManager databaseManager;

    @Inject
    private SoundEngine soundEngine;

    @Inject
    private VoiceoverHandler voiceoverHandler;

    @Inject
    private QuestListIndicatorManager questListIndicatorManager;

    private String playerName;

    @Override
    protected void startUp() {
        eventBus.register(soundEngine);
        executor.submit(this::initializeDatabase);
        log.info("Quest Voiceover plugin started");
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(soundEngine);
        databaseManager.closeConnection();
        log.info("Quest Voiceover plugin stopped");
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.DIALOG) {
            return;
        }

        initializePlayerNameIfNeeded();
        voiceoverHandler.handleDialogMessage(event.getMessage(), playerName);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuOption().equals("Continue")) {
            voiceoverHandler.stopVoiceover();
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (isDialogWidget(event.getGroupId())) {
            voiceoverHandler.handleDialogOpened();
        }

        if (event.getGroupId() == InterfaceID.QUEST_LIST) {
            questListIndicatorManager.onQuestListOpened();
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (isDialogWidget(event.getGroupId())) {
            voiceoverHandler.stopVoiceover();
        }

        if (event.getGroupId() == InterfaceID.QUEST_LIST) {
            questListIndicatorManager.onQuestListClosed();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        questListIndicatorManager.onGameTick();
    }

    @Provides
    QuestVoiceoverConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(QuestVoiceoverConfig.class);
    }

    private void initializeDatabase() {
        DatabaseVersionManager.prepareDatabaseSource(okHttpClient);
        databaseManager.initializeConnection();
        questListIndicatorManager.setVoicedQuests(databaseManager.getVoicedQuests());
        log.info("Database initialized");
    }

    private void initializePlayerNameIfNeeded() {
        if (playerName == null) {
            playerName = client.getLocalPlayer().getName();
        }
    }

    private boolean isDialogWidget(int groupId) {
        return groupId == InterfaceID.DIALOG_NPC
            || groupId == InterfaceID.DIALOG_PLAYER
            || groupId == InterfaceID.DIALOG_OPTION;
    }
}
