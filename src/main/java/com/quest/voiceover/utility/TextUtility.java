package com.quest.voiceover.utility;

public final class TextUtility {

    private static final String OSRS_COLOR_TAG_PATTERN = "<col=[^>]*>";
    private static final String OSRS_COLOR_CLOSE_TAG = "</col>";
    private static final String OSRS_LINE_BREAK = "<br>";

    private TextUtility() {}

    public static String stripColorTags(String text) {
        return text
            .replaceAll(OSRS_COLOR_TAG_PATTERN, "")
            .replace(OSRS_COLOR_CLOSE_TAG, "");
    }

    public static String stripLineBreaks(String text) {
        return text.replace(OSRS_LINE_BREAK, " ");
    }

    public static String stripAllTags(String text) {
        return text.replaceAll("<[^>]+>", "");
    }

    public static String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    public static String cleanForMatching(String text) {
        return normalizeWhitespace(stripAllTags(text));
    }

    public static String reapplyColorAfterLineBreaks(String text, String colorTag) {
        return text.replace(OSRS_LINE_BREAK, OSRS_COLOR_CLOSE_TAG + OSRS_LINE_BREAK + colorTag);
    }
}
