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
        log.info("play() called with fileName: '{}'", fileName);
        stopPlayback();

        if (fileName == null || fileName.isEmpty()) {
            log.warn("Attempted to play null or empty fileName");
            return;
        }

        URL soundUrl = buildSoundUrl(fileName);
        log.info("Built sound URL: {}", soundUrl);

        MP3Player currentPlayer = getOrCreatePlayer();
        log.info("Got MP3Player instance - isPlaying: {}, isStopped: {}, isPaused: {}",
            currentPlayer.isPlaying(), currentPlayer.isStopped(), currentPlayer.isPaused());

        int volume = config.mute() ? 0 : config.volume();
        log.info("Setting volume to {} (muted: {})", volume, config.mute());

        currentPlayer.setVolume(volume);
        log.info("Adding URL to player playlist: {}", soundUrl);
        currentPlayer.add(soundUrl);
        log.info("Calling player.play() - initiating download/stream from: {}", soundUrl);
        currentPlayer.play();

        log.info("After play() - isPlaying: {}, isStopped: {}, isPaused: {}",
            currentPlayer.isPlaying(), currentPlayer.isStopped(), currentPlayer.isPaused());

        soundPlaying = true;
        playbackStartTick = client.getTickCount();
        log.info("Playback initiated at tick {} for file: {}", playbackStartTick, fileName);
    }

    public void stop() {
        int currentTick = client.getTickCount();
        boolean pastGracePeriod = currentTick > playbackStartTick + PLAYBACK_GRACE_PERIOD_TICKS;
        log.info("stop() called - currentTick: {}, playbackStartTick: {}, pastGracePeriod: {}",
            currentTick, playbackStartTick, pastGracePeriod);
        if (pastGracePeriod) {
            stopPlayback();
        }
    }

    public void stopImmediately() {
        log.info("stopImmediately() called - forcing stop");
        stopPlayback();
    }

    public void setVolume(int volume) {
        getOrCreatePlayer().setVolume(volume);
    }

    public boolean isPlaying() {
        return soundPlaying && player != null && !player.isStopped();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (player != null && soundPlaying) {
            log.info("onGameTick - player state: isPlaying={}, isStopped={}, isPaused={}, soundPlaying={}",
                player.isPlaying(), player.isStopped(), player.isPaused(), soundPlaying);
            if (player.isStopped()) {
                log.info("onGameTick - player stopped, setting soundPlaying=false");
                soundPlaying = false;
            }
        }
    }

    private void stopPlayback() {
        if (player == null) {
            log.info("stopPlayback() - player is null, nothing to stop");
            return;
        }

        boolean wasPlaying = player.isPlaying();
        log.info("stopPlayback() - wasPlaying: {}, soundPlaying: {}", wasPlaying, soundPlaying);

        soundPlaying = false;
        player.clearPlayList();

        if (wasPlaying) {
            player.stop();
            log.info("stopPlayback() - player stopped");
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
                    log.info("Creating new MP3Player instance");
                    player = new MP3Player();
                    log.info("MP3Player created successfully");
                }
            }
        }
        return player;
    }
}
