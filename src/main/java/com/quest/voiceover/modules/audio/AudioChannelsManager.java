package com.quest.voiceover.modules.audio;

import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AudioChannelsManager {

    // VarPlayer returns 0-100 percentage, but setters expect absolute values
    private static final int MUSIC_MAX = 255;
    private static final int SOUND_EFFECT_MAX = 127;
    private static final int AREA_SOUND_MAX = 127;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public int getMusicVolume() {
        return client.getVarpValue(VarPlayer.MUSIC_VOLUME);
    }

    public int getSoundEffectVolume() {
        return client.getVarpValue(VarPlayer.SOUND_EFFECT_VOLUME);
    }

    public int getAreaEffectVolume() {
        return client.getVarpValue(VarPlayer.AREA_EFFECT_VOLUME);
    }

    public void setMusicVolume(int percentage) {
        int absolute = percentage * MUSIC_MAX / 100;
        clientThread.invokeLater(() -> {
            client.setMusicVolume(absolute);
            return true;
        });
    }

    public void setSoundEffectVolume(int percentage) {
        int absolute = percentage * SOUND_EFFECT_MAX / 100;
        clientThread.invokeLater(() -> {
            client.getPreferences().setSoundEffectVolume(absolute);
            return true;
        });
    }

    public void setAreaEffectVolume(int percentage) {
        int absolute = percentage * AREA_SOUND_MAX / 100;
        clientThread.invokeLater(() -> {
            client.getPreferences().setAreaSoundEffectVolume(absolute);
            return true;
        });
    }

    public void setAllVolumes(int musicPercentage, int soundEffectPercentage, int areaEffectPercentage) {
        int musicAbsolute = musicPercentage * MUSIC_MAX / 100;
        int soundAbsolute = soundEffectPercentage * SOUND_EFFECT_MAX / 100;
        int areaAbsolute = areaEffectPercentage * AREA_SOUND_MAX / 100;

        clientThread.invokeLater(() -> {
            client.setMusicVolume(musicAbsolute);
            client.getPreferences().setSoundEffectVolume(soundAbsolute);
            client.getPreferences().setAreaSoundEffectVolume(areaAbsolute);
            return true;
        });
    }
}
