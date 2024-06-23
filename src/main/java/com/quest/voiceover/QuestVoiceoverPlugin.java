package com.quest.voiceover;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@PluginDescriptor(
	name = "Quest Voiceover"
)
public class QuestVoiceoverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private QuestVoiceoverConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private DialogEngine dialogEngine;

	private String playerName = null;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(soundEngine);
		eventBus.register(dialogEngine);
		log.info("Quest Voiceover plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(soundEngine);
		eventBus.unregister(dialogEngine);
		log.info("Quest Voiceover plugin stopped!");
	}


	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if(chatMessage.getType().equals(ChatMessageType.DIALOG)) {
			if(this.playerName == null) {
				this.playerName = this.client.getLocalPlayer().getName();
			}

			MessageUtils message = new MessageUtils(chatMessage.getMessage(), this.playerName);
			System.out.printf("ID: %s | Sender: %s | Message: %s \n", message.id, message.name, message.text);
			try{
				soundEngine.play(String.format("%s.mp3", message.id));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equals("Continue") && event.getMenuTarget().contains("Dialog"))
		{
			soundEngine.stop();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Check if the loaded widget is the dialog widget
		if (event.getGroupId() == WidgetID.DIALOG_NPC_GROUP_ID || event.getGroupId() == WidgetID.DIALOG_PLAYER_GROUP_ID || event.getGroupId() == WidgetID.DIALOG_OPTION_GROUP_ID)
		{
			dialogEngine.setDialogOpened(true);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if(dialogEngine.getDialogOpened() && !dialogEngine.isDialogOpen()) {
			dialogEngine.setDialogOpened(false);
			soundEngine.stop();
		}
	}

	@Provides
	QuestVoiceoverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(QuestVoiceoverConfig.class);
	}
}
