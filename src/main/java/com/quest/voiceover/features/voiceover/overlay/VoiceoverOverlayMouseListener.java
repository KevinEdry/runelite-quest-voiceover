package com.quest.voiceover.features.voiceover.overlay;

import net.runelite.client.input.MouseListener;

import java.awt.event.MouseEvent;

public class VoiceoverOverlayMouseListener implements MouseListener {

    private final VoiceoverOverlayHandler overlay;

    public VoiceoverOverlayMouseListener(VoiceoverOverlayHandler overlay) {
        this.overlay = overlay;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent) {
        if (overlay.handleClick(mouseEvent.getX(), mouseEvent.getY())) {
            mouseEvent.consume();
        }
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent) {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent) {
        return mouseEvent;
    }
}
