package com.quest.voiceover;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.EventBus;

import java.net.URL;

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
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private DialogEngine dialogEngine;

	@Inject
	private HttpUtils httpUtils;

	private String playerName = null;

	// This is the only way (that I could think of) for me to reference the message from the `onWidgetLoaded`.
	private String currentSoundFileName = null;


	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(soundEngine);
		log.info("Quest Voiceover plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(soundEngine);
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

			String fileName = String.format("%s.mp3", message.id);
			currentSoundFileName = fileName;

            soundEngine.play(fileName);
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
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		// Check if the loaded widget is the dialog widget
		if (widgetLoaded.getGroupId() == InterfaceID.DIALOG_NPC || widgetLoaded.getGroupId() == InterfaceID.DIALOG_PLAYER || widgetLoaded.getGroupId() == InterfaceID.DIALOG_OPTION)
		{
			dialogEngine.setDialogOpened(true);
			if(dialogEngine.isPlayerOrNpcDialogOpen()) {
				new Thread( new Runnable() {
					@Override
					public void run() {
						URL soundFileURL = HttpUtils.RAW_GITHUB_SOUND_URL.newBuilder().addPathSegment(currentSoundFileName).build().url();
						if(httpUtils.isUrlReachable(soundFileURL)) {
							Widget dialogWidget = dialogEngine.getPlayerOrNpcWidget();

							clientThread.invokeLater(()-> {
								dialogEngine.addMuteButton(dialogWidget);
							});
						}
					}
				}).start();
			}
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed widgetClosed) {
		if (widgetClosed.getGroupId() == InterfaceID.DIALOG_NPC || widgetClosed.getGroupId() == InterfaceID.DIALOG_PLAYER || widgetClosed.getGroupId() == InterfaceID.DIALOG_OPTION)
		{
			dialogEngine.setDialogOpened(false);
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
