package com.quest.voiceover.features.voiceover.overlay.controls;

import com.quest.voiceover.Constants;
import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.features.voiceover.overlay.OverlayControl;
import com.quest.voiceover.modules.audio.AudioManager;
import net.runelite.api.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SpriteManager;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MuteControl implements OverlayControl {

    private static final int ICON_SIZE = 20;

    private final QuestVoiceoverConfig config;
    private final ConfigManager configManager;
    private final AudioManager audioManager;
    private final SpriteManager spriteManager;

    private Rectangle bounds;

    public MuteControl(
            QuestVoiceoverConfig config,
            ConfigManager configManager,
            AudioManager audioManager,
            SpriteManager spriteManager
    ) {
        this.config = config;
        this.configManager = configManager;
        this.audioManager = audioManager;
        this.spriteManager = spriteManager;
    }

    @Override
    public void render(Graphics2D graphics, int x, int y, Point overlayOrigin) {
        BufferedImage icon = getMuteIcon();
        if (icon != null) {
            graphics.drawImage(icon, x, y, ICON_SIZE, ICON_SIZE, null);
        }
        bounds = new Rectangle(overlayOrigin.x + x, overlayOrigin.y + y, ICON_SIZE, ICON_SIZE);
    }

    @Override
    public boolean handleClick(int x, int y) {
        if (bounds == null || !bounds.contains(x, y)) {
            return false;
        }

        configManager.setConfiguration(Constants.PLUGIN_CONFIG_GROUP, "mute", !config.mute());
        audioManager.setVolume(config.mute() ? 0 : config.volume());
        return true;
    }

    @Override
    public int getWidth() {
        return ICON_SIZE;
    }

    private BufferedImage getMuteIcon() {
        int spriteId = config.mute() ? SpriteID.OPTIONS_MUSIC_DISABLED : SpriteID.OPTIONS_MUSIC_VOLUME;
        return spriteManager.getSprite(spriteId, 0);
    }
}
