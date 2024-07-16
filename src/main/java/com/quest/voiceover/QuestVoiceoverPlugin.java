package com.quest.voiceover;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;

import com.quest.voiceover.database.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.eventbus.EventBus;
import okhttp3.OkHttpClient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
	name = "Quest Voiceover"
)
public class QuestVoiceoverPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DatabaseManager databaseManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private DialogEngine dialogEngine;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ScheduledExecutorService executor;

	private String playerName = null;
	private Boolean isQuestDialog = false;
	private String questName = null;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(soundEngine);
		log.info("Quest Voiceover plugin started!");

		executor.submit(() -> DatabaseFileManager.prepareDatabaseSource(okHttpClient));
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(soundEngine);
		log.info("Quest Voiceover plugin stopped!");

		databaseManager.closeConnection();
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		if (chatMessage.getType().equals(ChatMessageType.DIALOG)) {
			if (this.playerName == null) {
				this.playerName = this.client.getLocalPlayer().getName();
			}

			MessageUtils message = new MessageUtils(chatMessage.getMessage(), this.playerName);

			try (PreparedStatement statement = databaseManager.prepareStatement("SELECT quest, uri FROM dialogs WHERE character = ? AND text MATCH ?")) {
				statement.setString(1, message.name.replace("'", "''"));
				statement.setString(2, message.text.replace("'", "''"));

				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						String fileName = resultSet.getString("uri");
						String questName = resultSet.getString("quest");

						this.questName = questName;

						if (fileName != null || questName != null) {
							isQuestDialog = true;
							soundEngine.play(fileName);
						} else {
							isQuestDialog = false;
							log.warn("Sound URI could not be found for line: {}", message.text);
						}
					} else {
						isQuestDialog = false;
						log.warn("No matching dialog found for line: {}", message.text);
					}
				}
			} catch (SQLException e) {
				isQuestDialog = false;
				log.error("Encountered an SQL error", e);
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
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		// Check if the loaded widget is the dialog widget
		if (widgetLoaded.getGroupId() == InterfaceID.DIALOG_NPC || widgetLoaded.getGroupId() == InterfaceID.DIALOG_PLAYER || widgetLoaded.getGroupId() == InterfaceID.DIALOG_OPTION)
		{
			dialogEngine.setDialogOpened(true);
			if(dialogEngine.isPlayerOrNpcDialogOpen() && isQuestDialog) {
				Widget dialogWidget = dialogEngine.getPlayerOrNpcWidget();
				dialogEngine.addMuteButton(dialogWidget);
				if(questName != null) {
					dialogEngine.addQuestNameText(dialogWidget, this.questName);
				}
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
