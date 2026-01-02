package com.quest.voiceover.features.voiceover.overlay.controls;

import com.quest.voiceover.features.voiceover.overlay.OverlayControl;
import com.quest.voiceover.modules.audio.AudioManager;
import com.quest.voiceover.modules.audio.AudioQueueManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SkipControl implements OverlayControl {

    private static final int ICON_SIZE = 20;

    private final AudioManager audioManager;
    private final AudioQueueManager audioQueueManager;
    private final BufferedImage nextIcon;

    private Rectangle bounds;

    public SkipControl(AudioManager audioManager, AudioQueueManager audioQueueManager) {
        this.audioManager = audioManager;
        this.audioQueueManager = audioQueueManager;
        this.nextIcon = ImageUtil.loadImageResource(getClass(), "/com/quest/voiceover/player/next.png");
    }

    @Override
    public void render(Graphics2D graphics, int x, int y, Point overlayOrigin) {
        graphics.drawImage(nextIcon, x, y, ICON_SIZE, ICON_SIZE, null);
        bounds = new Rectangle(overlayOrigin.x + x, overlayOrigin.y + y, ICON_SIZE, ICON_SIZE);

        renderQueueCount(graphics, x, y);
    }

    @Override
    public boolean handleClick(int x, int y) {
        if (bounds == null || !bounds.contains(x, y)) {
            return false;
        }

        audioManager.skipToNext();
        return true;
    }

    @Override
    public int getWidth() {
        return ICON_SIZE;
    }

    public boolean hasQueue() {
        return !audioQueueManager.isEmpty();
    }

    private void renderQueueCount(Graphics2D graphics, int buttonX, int buttonY) {
        int queueSize = audioQueueManager.size();
        String queueText = String.valueOf(queueSize);

        graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(11f));
        graphics.setColor(Color.WHITE);
        graphics.drawString(queueText, buttonX + ICON_SIZE - 4, buttonY + 2);
    }
}
