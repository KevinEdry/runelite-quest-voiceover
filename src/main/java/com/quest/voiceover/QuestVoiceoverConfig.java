package com.quest.voiceover;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.*;

@ConfigGroup("quest.voiceover")
public interface QuestVoiceoverConfig extends Config
{
	@Getter
	@RequiredArgsConstructor
	enum PlayerVoice
	{
		MALE("Player Male"),
		FEMALE("Player Female");

		private final String characterName;

		@Override
		public String toString()
		{
			return name().charAt(0) + name().substring(1).toLowerCase();
		}
	}

	@ConfigSection(
			name = "Voice Settings",
			description = "Voice configuration options",
			position = 5,
			closedByDefault = false
	)
	String voiceSettings = "voiceSettings";

	@ConfigItem(
			keyName = "playerVoice",
			name = "Player Voice",
			description = "Voice used for your character's dialog.",
			section = voiceSettings,
			position = 6
	)
	default PlayerVoice playerVoice()
	{
		return PlayerVoice.MALE;
	}

	@ConfigSection(
			name = "General",
			description = "General settings",
			position = 10,
			closedByDefault = false
	)
	String generalSettings = "generalSettings";

	@ConfigSection(
			name = "Speech Highlighting",
			description = "Settings for speech highlighting",
			position = 20,
			closedByDefault = false
	)
	String speechHighlightingSettings = "speechHighlightingSettings";

	@ConfigSection(
			name = "Quest List",
			description = "Settings for the quest list",
			position = 30,
			closedByDefault = false
	)
	String questListSettings = "questListSettings";

	@Range(min = 1, max = 100)
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Volume control for the voiceover sounds.",
			position = 11,
			section = generalSettings
	)
	default int volume() {
		return 75;
	}

	@ConfigItem(
			keyName = "mute",
			name = "Mute",
			description = "Mutes the voiceover sound.",
			section = generalSettings,
			position = 12
	)
	default boolean mute()
	{
		return false;
	}

	@ConfigItem(
			keyName = "audioDucking",
			name = "Audio Ducking",
			description = "Lowers game audio while voiceover plays.",
			section = generalSettings,
			position = 13
	)
	default boolean audioDucking()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
			keyName = "audioDuckingAmount",
			name = "Ducking Amount",
			description = "How much to lower game audio (0 = mute, 100 = no change).",
			section = generalSettings,
			position = 14
	)
	default int audioDuckingAmount()
	{
		return 25;
	}

	@ConfigItem(
			keyName = "audioQueuing",
			name = "Audio Queuing",
			description = "Queue voiceovers instead of interrupting. Unfinished lines continue playing when advancing dialog.",
			section = generalSettings,
			position = 15
	)
	default boolean audioQueuing()
	{
		return true;
	}

	@ConfigItem(
			keyName = "speechHighlighting",
			name = "Speech Highlighting",
			description = "Highlights dialog text word-by-word as it is spoken.",
			section = speechHighlightingSettings,
			position = 21
	)
	default boolean speechHighlighting()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			keyName = "speechHighlightColor",
			name = "Speech Highlight Color",
			description = "Color used to highlight words that have been spoken.",
			section = speechHighlightingSettings,
			position = 22
	)
	default java.awt.Color speechHighlightColor()
	{
		return new java.awt.Color(0xFF0054BA, true);
	}

	@ConfigItem(
			keyName = "showVoicedIndicator",
			name = "Toggle Voiced Quest Indicator",
			description = "Shows [Voiced] prefix next to quests with voice acting in the quest list.",
			section = questListSettings,
			position = 31
	)
	default boolean showVoicedIndicator()
	{
		return true;
	}
}
