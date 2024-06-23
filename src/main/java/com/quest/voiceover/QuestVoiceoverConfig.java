package com.quest.voiceover;

import net.runelite.client.config.*;

@ConfigGroup("quest.voiceover")
public interface QuestVoiceoverConfig extends Config
{
	@ConfigSection(
			name = "General",
			description = "General settings",
			position = 20,
			closedByDefault = false
	)
	String generalSettings = "generalSettings";



	@Range(min = 1, max = 100)
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Volume control for the voiceover sounds.",
			position = 21,
			section = generalSettings)
	default int volume() {
		return 75;
	}


	@ConfigItem(
			keyName = "mute",
			name = "Mute",
			description = "Mutes the voiceover sound.",
			section = generalSettings,
			position = 22

	)
	default boolean mute()
	{
		return false;
	}
}
