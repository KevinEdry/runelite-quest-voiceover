package com.quest.voiceover.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PluginVersionUtility {

    private static final String VERSION_RESOURCE = "/com/quest/voiceover/version.properties";
    private static final String UNKNOWN_VERSION = "Unknown";

    private static final String VERSION = load();

    private PluginVersionUtility() {}

    public static String get() {
        return VERSION;
    }

    public static boolean isKnown() {
        return !UNKNOWN_VERSION.equals(VERSION);
    }

    private static String load() {
        try (InputStream input = PluginVersionUtility.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("version", UNKNOWN_VERSION);
            }
        } catch (IOException ignored) {
        }
        return UNKNOWN_VERSION;
    }
}
