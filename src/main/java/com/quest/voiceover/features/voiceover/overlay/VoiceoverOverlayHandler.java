package com.quest.voiceover.features.voiceover.overlay;

import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.features.voiceover.overlay.controls.CloseControl;
import com.quest.voiceover.features.voiceover.overlay.controls.MuteControl;
import com.quest.voiceover.features.voiceover.overlay.controls.PlayPauseControl;
import com.quest.voiceover.features.voiceover.overlay.controls.SkipControl;
import com.quest.voiceover.modules.audio.AudioManager;
import com.quest.voiceover.modules.audio.AudioQueueManager;
import com.quest.voiceover.modules.audio.QueuedAudio;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VoiceoverOverlayHandler extends Overlay {

    private static final int PADDING = 6;
    private static final int TEXT_HEIGHT = 12;
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 150);

    private final QuestVoiceoverConfig config;
    private final AudioManager audioManager;
    private final AudioQueueManager audioQueueManager;
    private final Client client;

    private final PlayPauseControl playPauseControl;
    private final MuteControl muteControl;
    private final SkipControl skipControl;
    private final CloseControl closeControl;

    @Inject
    public VoiceoverOverlayHandler(
            QuestVoiceoverConfig config,
            ConfigManager configManager,
            AudioManager audioManager,
            AudioQueueManager audioQueueManager,
            SpriteManager spriteManager,
            Client client
    ) {
        this.config = config;
        this.audioManager = audioManager;
        this.audioQueueManager = audioQueueManager;
        this.client = client;

        this.playPauseControl = new PlayPauseControl(audioManager);
        this.muteControl = new MuteControl(config, configManager, audioManager, spriteManager);
        this.skipControl = new SkipControl(audioManager, audioQueueManager);
        this.closeControl = new CloseControl(audioManager, spriteManager);

        setPosition(OverlayPosition.BOTTOM_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPreferredLocation(new Point(0, 500));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!shouldShowOverlay()) {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeSmallFont());

        String questName = getQuestName();
        String characterName = getCharacterName();
        List<OverlayControl> activeControls = getActiveControls();

        Dimension panelSize = calculatePanelSize(graphics, questName, characterName, activeControls);
        int textWidth = calculateTextWidth(graphics, questName, characterName);
        Point overlayOrigin = new Point(getBounds().x, getBounds().y);

        renderBackground(graphics, panelSize);
        renderCloseButton(graphics, panelSize.width, overlayOrigin);
        renderLabels(graphics, questName, characterName);
        renderControls(graphics, activeControls, textWidth, panelSize.height, overlayOrigin);

        return panelSize;
    }

    public boolean handleClick(int x, int y) {
        if (closeControl.handleClick(x, y)) {
            return true;
        }

        for (OverlayControl control : getActiveControls()) {
            if (control.handleClick(x, y)) {
                return true;
            }
        }

        return false;
    }

    private List<OverlayControl> getActiveControls() {
        List<OverlayControl> controls = new ArrayList<>();
        controls.add(playPauseControl);
        controls.add(muteControl);

        if (skipControl.hasQueue()) {
            controls.add(skipControl);
        }

        return controls;
    }

    private String getQuestName() {
        QueuedAudio current = audioQueueManager.getCurrentlyPlaying();
        return current != null ? current.getQuestName() : "";
    }

    private String getCharacterName() {
        QueuedAudio current = audioQueueManager.getCurrentlyPlaying();
        String characterName = current != null ? current.getCharacterName() : null;

        if (characterName == null || "Player".equals(characterName)) {
            if (client.getLocalPlayer() != null) {
                return client.getLocalPlayer().getName();
            }
            return "Player";
        }
        return characterName;
    }

    private int calculateTextWidth(Graphics2D graphics, String questName, String characterName) {
        FontMetrics metrics = graphics.getFontMetrics();
        int questWidth = metrics.stringWidth(questName);
        int characterWidth = metrics.stringWidth(characterName);
        return Math.max(questWidth, characterWidth);
    }

    private Dimension calculatePanelSize(Graphics2D graphics, String questName, String characterName, List<OverlayControl> controls) {
        int textWidth = calculateTextWidth(graphics, questName, characterName);
        int buttonsWidth = calculateControlsWidth(controls);

        int totalWidth = textWidth + PADDING * 3 + buttonsWidth + closeControl.getWidth() + PADDING;
        int totalHeight = PADDING * 2 + TEXT_HEIGHT * 2;

        return new Dimension(totalWidth, totalHeight);
    }

    private int calculateControlsWidth(List<OverlayControl> controls) {
        int width = 0;
        for (int i = 0; i < controls.size(); i++) {
            width += controls.get(i).getWidth();
            if (i < controls.size() - 1) {
                width += PADDING;
            }
        }
        return width;
    }

    private void renderBackground(Graphics2D graphics, Dimension size) {
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRoundRect(0, 0, size.width, size.height, 5, 5);
    }

    private void renderCloseButton(Graphics2D graphics, int panelWidth, Point overlayOrigin) {
        int closeX = panelWidth - closeControl.getWidth() - 2;
        int closeY = 2;
        closeControl.render(graphics, closeX, closeY, overlayOrigin);
    }

    private void renderLabels(Graphics2D graphics, String questName, String characterName) {
        int textY = PADDING + TEXT_HEIGHT - 2;

        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(Color.YELLOW);
        graphics.drawString(questName, PADDING, textY);

        graphics.setColor(Color.WHITE);
        graphics.drawString(characterName, PADDING, textY + TEXT_HEIGHT);
    }

    private void renderControls(Graphics2D graphics, List<OverlayControl> controls, int textWidth, int panelHeight, Point overlayOrigin) {
        int x = textWidth + PADDING * 2;
        int y = (panelHeight - 20) / 2;

        for (OverlayControl control : controls) {
            control.render(graphics, x, y, overlayOrigin);
            x += control.getWidth() + PADDING;
        }
    }

    private boolean shouldShowOverlay() {
        return config.showVoiceoverOverlay() && audioManager.isPlaying();
    }
}
