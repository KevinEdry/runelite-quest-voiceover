package com.quest.voiceover.modules.audio;

import com.quest.voiceover.QuestVoiceoverConfig;
import net.runelite.api.Client;
import net.runelite.api.GameState;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AudioDuckingManager {

    @Inject
    private Client client;

    @Inject
    private AudioChannelsManager audioChannelsManager;

    @Inject
    private QuestVoiceoverConfig config;

    private int originalMusicVolume = -1;
    private int originalSoundEffectVolume = -1;
    private int originalAreaEffectVolume = -1;
    private boolean isDucked = false;

    public void duck() {
        if (!config.audioDucking() || isDucked) {
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        originalMusicVolume = audioChannelsManager.getMusicVolume();
        originalSoundEffectVolume = audioChannelsManager.getSoundEffectVolume();
        originalAreaEffectVolume = audioChannelsManager.getAreaEffectVolume();

        int duckPercent = config.audioDuckingAmount();
        int duckedMusic = Math.max(1, originalMusicVolume * duckPercent / 100);
        int duckedSound = Math.max(1, originalSoundEffectVolume * duckPercent / 100);
        int duckedArea = Math.max(1, originalAreaEffectVolume * duckPercent / 100);

        audioChannelsManager.setAllVolumes(duckedMusic, duckedSound, duckedArea);
        isDucked = true;
    }

    public void restore() {
        if (!isDucked) {
            return;
        }

        audioChannelsManager.setAllVolumes(originalMusicVolume, originalSoundEffectVolume, originalAreaEffectVolume);

        originalMusicVolume = -1;
        originalSoundEffectVolume = -1;
        originalAreaEffectVolume = -1;
        isDucked = false;
    }
}
