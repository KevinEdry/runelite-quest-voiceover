package com.voiceover;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

public class SoundPlayer
{
    private static Clip clip;

    public static boolean soundFileExist(String soundFileName) {
        try (InputStream audioSrc = SoundPlayer.class.getResourceAsStream("/" + soundFileName))
        {
            return audioSrc != null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public static void playSound(String soundFileName)
    {
        stopSound();  // Stop any currently playing sound before starting a new one
        try (InputStream audioSrc = SoundPlayer.class.getResourceAsStream("/" + soundFileName))
        {
            if (audioSrc == null)
            {
                System.err.println("Sound file not found: " + soundFileName);
                return;
            }
            try (InputStream bufferedIn = new java.io.BufferedInputStream(audioSrc))
            {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
        {
            e.printStackTrace();
        }
    }

    public static void stopSound()
    {
        if (clip != null && clip.isRunning())
        {
            clip.stop();
            clip.close();
        }
    }
}