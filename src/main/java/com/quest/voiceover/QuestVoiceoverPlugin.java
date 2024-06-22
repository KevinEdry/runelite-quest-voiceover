package com.quest.voiceover;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "QuestVoiceoverPlugin"
)
public class QuestVoiceoverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private QuestVoiceoverConfig config;
//
//	@Inject
//	private ScheduledExecutorService executor;

	private String playerName = null;

	@Inject
	private SoundEngine soundEngine;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}


	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if(chatMessage.getType().equals(ChatMessageType.DIALOG)) {
			if(this.playerName == null) {
				this.playerName = this.client.getLocalPlayer().getName();
			}

			MessageUtils message = new MessageUtils(chatMessage.getMessage(), this.playerName);
//			System.out.printf(chatMessage.getMessage());
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
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();
		if (action.equals(MenuAction.WALK))
		{
			soundEngine.stop();
		}
	}

	@Provides
	QuestVoiceoverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(QuestVoiceoverConfig.class);
	}
}
