package com.voiceover;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "VoiceoverPlugin"
)
public class VoiceoverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private VoiceoverConfig config;

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
			MessageUtils message = new MessageUtils(chatMessage.getMessage());
			System.out.printf(chatMessage.getMessage());
//			System.out.printf("ID: %s | Sender: %s | Message: %s \n", message.id, message.name, message.text);
			if(SoundPlayer.soundFileExist(String.format("%s.wav", message.id))) {
				SoundPlayer.playSound(String.format("%s.wav", message.id));
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

		if (action.equals(MenuAction.WIDGET_CONTINUE) || action.equals(MenuAction.WALK))
		{
			SoundPlayer.stopSound();
		}
	}

	@Provides
	VoiceoverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(VoiceoverConfig.class);
	}
}
