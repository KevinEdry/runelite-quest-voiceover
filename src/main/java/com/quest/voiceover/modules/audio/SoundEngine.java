package com.quest.voiceover.modules.audio;

import com.quest.voiceover.QuestVoiceoverConfig;
import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import okhttp3.HttpUrl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;

@Slf4j
@Singleton
public class SoundEngine {

    private static final HttpUrl RAW_GITHUB_SOUND_BRANCH_URL =
        HttpUrl.parse("https://github.com/KevinEdry/runelite-quest-voiceover/raw/sounds");
    private static final int PLAYBACK_GRACE_PERIOD_TICKS = 1;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private Client client;

    private volatile MP3Player player;
    private volatile boolean soundPlaying;
    private int playbackStartTick;

    public void play(String fileName) {
        stopPlayback();

        URL soundUrl = buildSoundUrl(fileName);
        MP3Player currentPlayer = getOrCreatePlayer();

        currentPlayer.setVolume(config.mute() ? 0 : config.volume());
        currentPlayer.add(soundUrl);
        currentPlayer.play();

        soundPlaying = true;
        playbackStartTick = client.getTickCount();
    }

    public void stop() {
        boolean pastGracePeriod = client.getTickCount() > playbackStartTick + PLAYBACK_GRACE_PERIOD_TICKS;
        if (pastGracePeriod) {
            stopPlayback();
        }
    }

    public void setVolume(int volume) {
        getOrCreatePlayer().setVolume(volume);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (player != null && player.isStopped() && soundPlaying) {
            soundPlaying = false;
        }
    }

    private void stopPlayback() {
        if (player == null) {
            return;
        }

        soundPlaying = false;
        player.clearPlayList();

        if (player.isPlaying()) {
            player.stop();
        }
    }

    private URL buildSoundUrl(String fileName) {
        return RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
            .addPathSegment(fileName)
            .build()
            .url();
    }

    private MP3Player getOrCreatePlayer() {
        if (player == null) {
            synchronized (this) {
                if (player == null) {
                    player = new MP3Player();
                }
            }
        }
        return player;
    }
}
