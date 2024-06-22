package com.quest.voiceover;

import jaco.mp3.player.MP3Player;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class SoundEngine {

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
        File file = this.getFileFromResource(fileName);

        if (file.exists() && file.canRead()) {
            player.add(file);
            player.play();
        }
        else {
            System.out.println("File not found");
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
