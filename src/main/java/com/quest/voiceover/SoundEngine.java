package com.quest.voiceover;

import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import okhttp3.HttpUrl;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;

@Slf4j
@Singleton
public class SoundEngine {

    @Inject
    private QuestVoiceoverConfig config;

    private volatile MP3Player player;
    private Boolean soundPlaying = false;
    public static final HttpUrl RAW_GITHUB_SOUND_BRANCH_URL = HttpUrl.parse("https://github.com/KevinEdry/rl-voiceover/raw/database");


    public void play(String fileName) {
        stop();
        MP3Player player = getPlayer();

        HttpUrl httpUrl = RAW_GITHUB_SOUND_BRANCH_URL.newBuilder().addPathSegment(fileName).build();
        URL soundUrl = httpUrl.url();

        player.setVolume(config.mute() ? 0 : config.volume());
        player.addToPlayList(soundUrl);

        player.play();
        soundPlaying = true;
    }

    public void stop() {
        soundPlaying = false;
        if (player != null) {
            player.getPlayList().clear();
            player.stop();
        }
    }

    public void setVolume(int volume) {
         getPlayer().setVolume(volume);
    }

    public Boolean isSoundPlaying() {
        return player.isPlaying();
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
        if(this.player.isStopped() && soundPlaying) {
            // Detects when a sound have stopped playing.
            soundPlaying = false;
        }
    }
}
