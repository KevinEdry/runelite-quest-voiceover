package com.voiceover;

import jaco.mp3.player.MP3Player;
import okhttp3.HttpUrl;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class SoundEngine {

    private static HttpUrl RAW_GITHUB_SOUND_URL = HttpUrl.parse("https://github.com/KevinEdry/rl-voiceover/raw/sounds");
    private volatile MP3Player jacoPlayer;

    private File getFileFromResource(String fileName) throws URISyntaxException {

        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {

            // failed if files have whitespaces or special characters
            //return new File(resource.getFile());

            return new File(resource.toURI());
        }

    }


    public void play(String fileName) throws URISyntaxException {
        stop();
        MP3Player player = getJacoPlayer();

        HttpUrl httpUrl = RAW_GITHUB_SOUND_URL.newBuilder().addPathSegment(fileName).build();
        URL soundUrl = httpUrl.url();
        player.add(soundUrl);
        player.play();
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
