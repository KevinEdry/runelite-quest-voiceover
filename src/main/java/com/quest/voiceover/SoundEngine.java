package com.quest.voiceover;

import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import okhttp3.HttpUrl;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.time.Instant;
import java.time.Duration;

@Slf4j
@Singleton
public class SoundEngine {

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private Client client;

    private volatile MP3Player player;
    private volatile boolean soundPlaying = false;
    private int playbackStartTime = 0;
    private static final int PLAYBACK_GRACE_PERIOD = 1; // Adjust as needed
    public static final HttpUrl RAW_GITHUB_SOUND_BRANCH_URL = HttpUrl.parse("https://github.com/KevinEdry/runelite-quest-voiceover/raw/sounds");

    public void play(String fileName) {
        stopPlayback();
        MP3Player player = getPlayer();

        assert RAW_GITHUB_SOUND_BRANCH_URL != null;
        HttpUrl httpUrl = RAW_GITHUB_SOUND_BRANCH_URL.newBuilder().addPathSegment(fileName).build();
        URL soundUrl = httpUrl.url();

        player.setVolume(config.mute() ? 0 : config.volume());
        player.add(soundUrl);

        player.play();
        soundPlaying = true;
        playbackStartTime = client.getTickCount();
    }

    public void stop() {
        if (client.getTickCount() > playbackStartTime + PLAYBACK_GRACE_PERIOD ) {
            stopPlayback();
        }
    }

    private void stopPlayback() {
        if (player != null) {
            soundPlaying = false;
            player.clearPlayList();
            if (player.isPlaying()) {
                player.stop();
            }
        }
    }

    public void setVolume(int volume) {
        getPlayer().setVolume(volume);
    }

    public boolean isSoundPlaying() {
        return player != null && player.isPlaying();
    }

    private MP3Player getPlayer() {
        MP3Player player = this.player;
        if (player == null) {
            synchronized (this) {
                player = this.player;
                if (player == null) {
                    player = this.player = new MP3Player();
                }
            }
        }
        return player;
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if(this.player != null && this.player.isStopped() && soundPlaying) {
            // Detects when a sound have stopped playing.
            soundPlaying = false;
        }
    }
}