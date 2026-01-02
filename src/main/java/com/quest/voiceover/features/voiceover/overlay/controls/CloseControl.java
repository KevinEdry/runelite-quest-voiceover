package com.quest.voiceover.features.voiceover.overlay.controls;

import com.quest.voiceover.features.voiceover.overlay.OverlayControl;
import com.quest.voiceover.modules.audio.AudioManager;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CloseControl implements OverlayControl {

    private static final int ICON_SIZE = 10;

    private final AudioManager audioManager;
    private final SpriteManager spriteManager;

    private Rectangle bounds;

    public CloseControl(AudioManager audioManager, SpriteManager spriteManager) {
        this.audioManager = audioManager;
        this.spriteManager = spriteManager;
    }

    @Override
    public void render(Graphics2D graphics, int x, int y, Point overlayOrigin) {
        BufferedImage closeIcon = spriteManager.getSprite(SpriteID.WINDOW_CLOSE_BUTTON, 0);
        if (closeIcon != null) {
            graphics.drawImage(closeIcon, x, y, ICON_SIZE, ICON_SIZE, null);
        }
        bounds = new Rectangle(overlayOrigin.x + x, overlayOrigin.y + y, ICON_SIZE, ICON_SIZE);
    }

    @Override
    public boolean handleClick(int x, int y) {
        if (bounds != null && bounds.contains(x, y)) {
            audioManager.stopAll();
            return true;
        }
        return false;
    }

    @Override
    public int getWidth() {
        return ICON_SIZE;
    }
}
