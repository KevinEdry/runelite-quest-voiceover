package com.quest.voiceover;

import net.runelite.client.config.*;

@ConfigGroup("quest.voiceover")
public interface QuestVoiceoverConfig extends Config
{
	@ConfigSection(
			name = "General",
			description = "General settings",
			position = 10,
			closedByDefault = false
	)
	String generalSettings = "generalSettings";

	@ConfigSection(
			name = "Quest Dialog",
			description = "Settings for the quest dialog overlay",
			position = 20,
			closedByDefault = false
	)
	String questDialogSettings = "questDialogSettings";

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
			keyName = "showMuteButton",
			name = "Toggle Mute Button",
			description = "Shows the mute button on the quest dialog.",
			section = questDialogSettings,
			position = 21
	)
	default boolean showMuteButton()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showQuestName",
			name = "Toggle Quest Name",
			description = "Shows the quest name on the quest dialog.",
			section = questDialogSettings,
			position = 23
	)
	default boolean showQuestName()
	{
		return true;
	}

	@ConfigItem(
			keyName = "speechHighlighting",
			name = "Speech Highlighting",
			description = "Highlights dialog text word-by-word as it is spoken.",
			section = questDialogSettings,
			position = 24
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
			section = questDialogSettings,
			position = 25
	)
	default java.awt.Color speechHighlightColor()
	{
		return new java.awt.Color(0, 255, 0);
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
