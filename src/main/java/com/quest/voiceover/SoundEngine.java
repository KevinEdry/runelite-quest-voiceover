package com.quest.voiceover;

import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.eventbus.Subscribe;

import okhttp3.HttpUrl;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URISyntaxException;
import java.net.URL;

@Slf4j
@Singleton
public class SoundEngine {

    @Inject
    private QuestVoiceoverConfig config;

    private static final HttpUrl RAW_GITHUB_SOUND_URL = HttpUrl.parse("https://github.com/KevinEdry/rl-voiceover/raw/sounds");
    private volatile MP3Player jacoPlayer;
    private Boolean soundPlaying = false;

    public void play(String fileName) throws URISyntaxException {
        stop();

        MP3Player player = getJacoPlayer();
        HttpUrl httpUrl = RAW_GITHUB_SOUND_URL.newBuilder().addPathSegment(fileName).build();
        URL soundUrl = httpUrl.url();

        try {
            player.setVolume(config.mute() ? 0 : config.volume());
            player.addToPlayList(soundUrl);
            player.play();
            soundPlaying = true;
        } catch (Exception e) {
            stop();
            log.warn("Sound file {}, doesn't exist.", fileName);
        }
    }

    public void stop() {
        soundPlaying = false;
        if (jacoPlayer != null) {
            jacoPlayer.stop();
            jacoPlayer.getPlayList().clear();
        }
    }

    public void setVolume(int volume) {
         getJacoPlayer().setVolume(volume);
    }

    private MP3Player getJacoPlayer() {
        MP3Player player = this.jacoPlayer;
        if (player == null) {
            synchronized (this) {
                player = this.jacoPlayer;
                if (player == null) {
                    player = this.jacoPlayer = new MP3Player();
                }
            }
        }
        return player;
    }

    private void onSoundStopped() {
        soundPlaying = false;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if(this.jacoPlayer.isStopped() && soundPlaying) {
            onSoundStopped();
        }
    }
}
