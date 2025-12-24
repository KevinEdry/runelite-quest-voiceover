package com.quest.voiceover.modules.audio;

import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.Constants;
import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;

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

    private volatile MP3Player player;
    private volatile boolean soundPlaying;
    private int playbackStartTick;

    public void play(String fileName) {
        stopPlayback();

        if (fileName == null || fileName.isEmpty()) {
            log.warn("Attempted to play null or empty fileName");
            return;
        }

        URL soundUrl = buildSoundUrl(fileName);
        MP3Player currentPlayer = getOrCreatePlayer();

        int volume = config.mute() ? 0 : config.volume();
        currentPlayer.setVolume(volume);
        currentPlayer.add(soundUrl);
        currentPlayer.play();

        audioDuckingManager.duck();
        soundPlaying = true;
        playbackStartTick = client.getTickCount();
        log.debug("Playing audio: {}", fileName);
    }

    public void stop() {
        int currentTick = client.getTickCount();
        boolean pastGracePeriod = currentTick > playbackStartTick + PLAYBACK_GRACE_PERIOD_TICKS;
        if (pastGracePeriod) {
            stopPlayback();
        }
    }

    public void stopImmediately() {
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
        if (player != null && soundPlaying && player.isStopped()) {
            soundPlaying = false;
            audioDuckingManager.restore();
        }
    }

    private void stopPlayback() {
        if (player == null) {
            return;
        }

        boolean wasPlaying = player.isPlaying();
        soundPlaying = false;
        player.clearPlayList();

        if (wasPlaying) {
            player.stop();
            audioDuckingManager.restore();
        }
    }

    private URL buildSoundUrl(String fileName) {
        return Constants.RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
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
