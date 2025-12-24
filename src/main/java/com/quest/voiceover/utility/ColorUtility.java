package com.quest.voiceover.utility;

import java.awt.Color;

public final class ColorUtility {

    public static final int WHITE = 0xFFFFFF;
    public static final int BLACK = 0x000000;

    private ColorUtility() {}

    public static String toHex(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String toOsrsColorTag(Color color) {
        return "<col=" + toHex(color) + ">";
    }

    public static String toOsrsColorTag(String hexColor) {
        return "<col=" + hexColor + ">";
    }

    public static String wrapWithColor(String text, Color color) {
        return toOsrsColorTag(color) + text + "</col>";
    }

    public static String wrapWithColor(String text, String hexColor) {
        return toOsrsColorTag(hexColor) + text + "</col>";
    }
}
