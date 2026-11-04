package com.quest.voiceover.modules.audio;

import com.quest.voiceover.Constants;
import com.quest.voiceover.QuestVoiceoverConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class AudioManager {

    private static final int PLAYBACK_GRACE_PERIOD_TICKS = 1;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private Client client;

    @Inject
    private AudioDuckingManager audioDuckingManager;

    @Inject
    private AudioQueueManager audioQueueManager;

    private final StreamingMp3Player audioPlayer;
    private int playbackStartTick;

    @Inject
    private AudioManager(OkHttpClient okHttpClient) {
        this.audioPlayer = new StreamingMp3Player(okHttpClient, this::onPlaybackComplete);
    }

    public int getPlaybackPositionMillis() {
        return audioPlayer.getPositionMillis();
    }

    public void play(String fileName, String questName, String characterName) {
        if (fileName == null || fileName.isEmpty()) {
            log.warn("Attempted to play null or empty fileName");
            return;
        }

        if (shouldQueueAdditionalLines() && isPlaying()) {
            audioQueueManager.add(fileName, questName, characterName);
            return;
        }

        audioQueueManager.clear();
        audioQueueManager.setCurrentlyPlaying(fileName, questName, characterName);

        playbackStartTick = client.getTickCount();

        startPlayback(fileName);
        audioDuckingManager.duck();
    }

    public void pause() {
        audioPlayer.pause();
        log.info("Audio paused");
    }

    public void resume() {
        audioPlayer.resume();
        log.info("Audio resumed");
    }

    public boolean isPaused() {
        return audioPlayer.isPaused();
    }

    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    public void stop() {
        if (shouldPreserveActivePlayback()) {
            return;
        }

        int currentTick = client.getTickCount();
        boolean pastGracePeriod = currentTick > playbackStartTick + PLAYBACK_GRACE_PERIOD_TICKS;
        if (pastGracePeriod) {
            stopAll();
        }
    }

    public void stopImmediately() {
        if (shouldPreserveActivePlayback()) {
            return;
        }
        stopAll();
    }

    /**
     * Queuing stacks every advancing line so each plays in full. When the user opts to
     * finish only the last line, advancing must interrupt instead so audio stays in sync
     * with the dialog they are clicking through.
     */
    private boolean shouldQueueAdditionalLines() {
        return config.audioQueuing() && !config.finishLastLine();
    }

    /**
     * Both queuing and finish-last-line let the currently playing line ride out when the
     * dialog closes, so leaving an NPC does not cut the voiceover off mid-sentence.
     */
    private boolean shouldPreserveActivePlayback() {
        return config.audioQueuing() || config.finishLastLine();
    }

    public void stopAll() {
        audioQueueManager.clear();
        audioPlayer.stop();
        audioDuckingManager.restore();
    }

    public void skipToNext() {
        if (!isPlaying()) {
            return;
        }

        QueuedAudio next = audioQueueManager.poll();
        if (next == null) {
            stopAll();
            return;
        }

        log.info("Skipping to next: {}", next.getFileName());
        audioQueueManager.setCurrentlyPlaying(next.getFileName(), next.getQuestName(), next.getCharacterName());
        startPlayback(next.getFileName());
    }

    public boolean isPlaying() {
        return audioPlayer.isPlaying() || !audioQueueManager.isEmpty();
    }

    private void onPlaybackComplete() {
        QueuedAudio next = audioQueueManager.poll();
        if (next == null) {
            audioDuckingManager.restore();
            return;
        }

        log.info("Playing next from queue: {}", next.getFileName());
        audioQueueManager.setCurrentlyPlaying(next.getFileName(), next.getQuestName(), next.getCharacterName());
        startPlayback(next.getFileName());
    }

    private void startPlayback(String fileName) {
        audioPlayer.setVolume(config.mute() ? 0 : config.volume());
        audioPlayer.setSpeed(config.playbackSpeed().getMultiplier());
        audioPlayer.play(soundUrl(fileName));
        log.info("Playing audio: {}", fileName);
    }

    private HttpUrl soundUrl(String fileName) {
        return Constants.RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
            .addPathSegment(fileName)
            .build();
    }
}
