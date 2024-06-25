package com.quest.voiceover;

import okhttp3.HttpUrl;

import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public static final HttpUrl RAW_GITHUB_SOUND_URL = HttpUrl.parse("https://github.com/KevinEdry/rl-voiceover/raw/sounds");

    public boolean isUrlReachable(URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.getInputStream().close();

            return true;
        } catch ( Exception e ) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
