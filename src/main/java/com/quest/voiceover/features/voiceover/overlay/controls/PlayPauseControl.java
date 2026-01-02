package com.quest.voiceover.features.voiceover.overlay.controls;

import com.quest.voiceover.features.voiceover.overlay.OverlayControl;
import com.quest.voiceover.modules.audio.AudioManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PlayPauseControl implements OverlayControl {

    private static final int ICON_SIZE = 20;

    private final AudioManager audioManager;
    private final BufferedImage playIcon;
    private final BufferedImage pauseIcon;

    private Rectangle bounds;

    public PlayPauseControl(AudioManager audioManager) {
        this.audioManager = audioManager;
        this.playIcon = ImageUtil.loadImageResource(getClass(), "/com/quest/voiceover/player/play.png");
        this.pauseIcon = ImageUtil.loadImageResource(getClass(), "/com/quest/voiceover/player/pause.png");
    }

    @Override
    public void render(Graphics2D graphics, int x, int y, Point overlayOrigin) {
        BufferedImage icon = audioManager.isPaused() ? playIcon : pauseIcon;
        graphics.drawImage(icon, x, y, ICON_SIZE, ICON_SIZE, null);
        bounds = new Rectangle(overlayOrigin.x + x, overlayOrigin.y + y, ICON_SIZE, ICON_SIZE);
    }

    @Override
    public boolean handleClick(int x, int y) {
        if (bounds != null && bounds.contains(x, y)) {
            if (audioManager.isPaused()) {
                audioManager.resume();
            } else {
                audioManager.pause();
            }
            return true;
        }
        return false;
    }

    @Override
    public int getWidth() {
        return ICON_SIZE;
    }
}
