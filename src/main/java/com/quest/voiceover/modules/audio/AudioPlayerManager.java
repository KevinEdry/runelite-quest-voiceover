package com.quest.voiceover.modules.audio;

import com.quest.voiceover.Constants;
import jaco.mp3.player.MP3Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AudioPlayerManager {

    private static final int STOPPED_CHECK_INTERVAL_MS = 100;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Runnable onPlaybackComplete;

    private volatile MP3Player player;

    @Getter
    private volatile boolean playing;

    @Getter
    private volatile boolean paused;

    public AudioPlayerManager(Runnable onPlaybackComplete) {
        this.onPlaybackComplete = onPlaybackComplete;
    }

    public void play(String fileName) {
        executor.submit(() -> {
            try {
                stopInternal();
                playInternal(fileName);
            } catch (Exception exception) {
                log.error("Failed to play audio: {}", fileName, exception);
                playing = false;
            }
        });
    }

    public void pause() {
        if (!playing || paused) {
            return;
        }
        paused = true;
        executor.submit(() -> {
            if (player != null) {
                player.pause();
            }
        });
        log.debug("Audio paused");
    }

    public void resume() {
        if (!playing || !paused) {
            return;
        }
        paused = false;
        executor.submit(() -> {
            if (player != null) {
                player.play();
            }
        });
        log.debug("Audio resumed");
    }

    public void stop() {
        playing = false;
        paused = false;
        executor.submit(this::stopInternal);
    }

    public void setVolume(int volume) {
        executor.submit(() -> {
            if (player != null) {
                player.setVolume(volume);
            }
        });
    }

    private void playInternal(String fileName) {
        URL soundUrl = buildSoundUrl(fileName);
        MP3Player currentPlayer = getOrCreatePlayer();

        currentPlayer.add(soundUrl);
        currentPlayer.play();

        playing = true;
        paused = false;

        log.info("Playing audio: {}", fileName);
        scheduleStoppedCheck();
    }

    private void stopInternal() {
        if (player == null) {
            return;
        }
        player.stop();
        player.clearPlayList();
    }

    private void scheduleStoppedCheck() {
        executor.schedule(() -> {
            if (player == null || !playing || paused) {
                return;
            }

            if (!player.isStopped()) {
                scheduleStoppedCheck();
                return;
            }

            playing = false;
            if (onPlaybackComplete != null) {
                onPlaybackComplete.run();
            }
        }, STOPPED_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private URL buildSoundUrl(String fileName) {
        return Constants.RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
            .addPathSegment(fileName)
            .build()
            .url();
    }

    private MP3Player getOrCreatePlayer() {
        if (player != null) {
            return player;
        }

        synchronized (this) {
            if (player == null) {
                player = new MP3Player();
            }
        }
        return player;
    }
}
