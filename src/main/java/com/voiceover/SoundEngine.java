package com.voiceover;

import jaco.mp3.player.MP3Player;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

@Slf4j
public class SoundEngine {

    private static final HttpUrl RAW_GITHUB_SOUND_URL = HttpUrl.parse("https://github.com/KevinEdry/rl-voiceover/raw/sounds");
    private volatile MP3Player jacoPlayer;

    public void play(String fileName) throws URISyntaxException {
        stop();

        MP3Player player = getJacoPlayer();
        HttpUrl httpUrl = RAW_GITHUB_SOUND_URL.newBuilder().addPathSegment(fileName).build();
        URL soundUrl = httpUrl.url();

        try {
            player.add(soundUrl);
            player.play();
        } catch (Exception e) {
            stop();
            log.warn("Sound file {}, doesn't exist.", fileName);
        }
    }

    public void stop() {
        if (jacoPlayer != null) {
            jacoPlayer.stop();
            jacoPlayer.getPlayList().clear();
        }
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

}
