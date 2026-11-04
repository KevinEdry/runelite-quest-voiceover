package com.quest.voiceover.modules.audio;

import com.quest.voiceover.Constants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AudioPlayerManager {

    private static final int STOPPED_CHECK_INTERVAL_MS = 100;
    private static final int DEFAULT_VOLUME = 100;
    private static final double NORMAL_SPEED = 1.0;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Runnable onPlaybackComplete;
    private final OkHttpClient okHttpClient;
    private final AtomicInteger playbackEpoch = new AtomicInteger();

    private volatile AudioPlayer player;
    private volatile int volume = DEFAULT_VOLUME;
    private volatile double speed = NORMAL_SPEED;

    @Getter
    private volatile boolean playing;

    @Getter
    private volatile boolean paused;

    public AudioPlayerManager(Runnable onPlaybackComplete, OkHttpClient okHttpClient) {
        this.onPlaybackComplete = onPlaybackComplete;
        this.okHttpClient = okHttpClient;
    }

    public int getPositionMillis() {
        AudioPlayer currentPlayer = player;
        return currentPlayer == null ? 0 : currentPlayer.getPositionMillis();
    }

    public void play(String fileName) {
        playing = true;
        paused = false;
        int epoch = playbackEpoch.incrementAndGet();
        executor.submit(() -> {
            try {
                stopInternal();
                playInternal(fileName, epoch);
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
        scheduleStoppedCheck(playbackEpoch.get());
        log.debug("Audio resumed");
    }

    public void stop() {
        playing = false;
        paused = false;
        executor.submit(this::stopInternal);
    }

    public void setVolume(int volume) {
        this.volume = volume;
        executor.submit(() -> {
            if (player != null) {
                player.setVolume(volume);
            }
        });
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        executor.submit(() -> {
            if (player != null) {
                player.setSpeed(speed);
            }
        });
    }

    private void playInternal(String fileName, int epoch) {
        if (!playing || epoch != playbackEpoch.get()) {
            return;
        }

        URL soundUrl = buildSoundUrl(fileName);
        AudioPlayer currentPlayer = getOrCreatePlayer();

        currentPlayer.setVolume(volume);
        currentPlayer.setSpeed(speed);
        currentPlayer.add(soundUrl);
        currentPlayer.play();

        log.info("Playing audio: {}", fileName);
        scheduleStoppedCheck(epoch);
    }

    private void stopInternal() {
        if (player == null) {
            return;
        }
        player.stop();
        player.clearPlayList();
    }

    private void scheduleStoppedCheck(int epoch) {
        executor.schedule(() -> {
            if (epoch != playbackEpoch.get() || player == null || !playing) {
                return;
            }

            if (player.isStopped()) {
                playing = false;
                paused = false;
                if (onPlaybackComplete != null) {
                    onPlaybackComplete.run();
                }
                return;
            }

            if (paused) {
                return;
            }

            scheduleStoppedCheck(epoch);
        }, STOPPED_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private URL buildSoundUrl(String fileName) {
        return Constants.RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
            .addPathSegment(fileName)
            .build()
            .url();
    }

    private AudioPlayer getOrCreatePlayer() {
        if (player != null) {
            return player;
        }

        synchronized (this) {
            if (player == null) {
                player = new AudioPlayer(okHttpClient);
            }
        }
        return player;
    }
}
