package com.quest.voiceover;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class QuestVoiceoverPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(QuestVoiceoverPlugin.class);
		RuneLite.main(args);
	}
}