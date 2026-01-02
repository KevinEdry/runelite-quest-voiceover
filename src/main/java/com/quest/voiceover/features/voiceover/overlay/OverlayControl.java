package com.quest.voiceover.features.voiceover.overlay;

import java.awt.*;

public interface OverlayControl {

    void render(Graphics2D graphics, int x, int y, Point overlayOrigin);

    boolean handleClick(int x, int y);

    int getWidth();
}
