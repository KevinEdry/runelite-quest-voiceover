package com.voiceover;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class VoiceoverPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(VoiceoverPlugin.class);
		RuneLite.main(args);
	}
}