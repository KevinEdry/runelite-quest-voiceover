package com.voiceover;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

public class SoundPlayer
{
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
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            }
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
        {
            e.printStackTrace();
        }
    }
}