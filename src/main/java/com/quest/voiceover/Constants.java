package com.quest.voiceover;

import okhttp3.HttpUrl;

public final class Constants {

    public static final String PLUGIN_CONFIG_GROUP = "quest.voiceover";

    public static final HttpUrl RAW_GITHUB_SOUND_BRANCH_URL =
        HttpUrl.get("https://github.com/KevinEdry/runelite-quest-voiceover/raw/sounds");

    public static final HttpUrl RAW_GITHUB_DATABASE_BRANCH_URL =
        HttpUrl.get("https://github.com/KevinEdry/runelite-quest-voiceover/raw/database");

    public static final int MP3_BITRATE_KBPS = 96;

    private Constants() {}
}
