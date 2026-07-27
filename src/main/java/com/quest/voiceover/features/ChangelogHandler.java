package com.quest.voiceover.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quest.voiceover.Constants;
import com.quest.voiceover.utility.PluginVersionUtility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Singleton
public class ChangelogHandler {

    private static final String LAST_SEEN_VERSION_KEY = "lastSeenVersion";
    private static final String RELEASE_NOTES_API_URL =
        "https://api.github.com/repos/KevinEdry/runelite-quest-voiceover/releases/tags/v";
    private static final String CHAT_PREFIX = "[Quest Voiceover] ";
    private static final int MAX_CHANGELOG_LINES = 20;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ScheduledExecutorService executor;

    private boolean checkedThisSession;

    /**
     * The changelog must appear once per new release rather than on every login.
     */
    public void checkForUpdate() {
        if (checkedThisSession) {
            return;
        }
        checkedThisSession = true;

        if (!PluginVersionUtility.isKnown()) {
            return;
        }

        String currentVersion = PluginVersionUtility.get();
        String lastSeenVersion = configManager.getConfiguration(Constants.PLUGIN_CONFIG_GROUP, LAST_SEEN_VERSION_KEY);

        configManager.setConfiguration(Constants.PLUGIN_CONFIG_GROUP, LAST_SEEN_VERSION_KEY, currentVersion);

        boolean freshInstall = lastSeenVersion == null || lastSeenVersion.isEmpty();
        if (freshInstall || currentVersion.equals(lastSeenVersion)) {
            return;
        }

        executor.submit(() -> fetchAndDisplayChangelog(currentVersion));
    }

    private void fetchAndDisplayChangelog(String version) {
        Request request = new Request.Builder()
            .url(RELEASE_NOTES_API_URL + version)
            .header("Accept", "application/vnd.github+json")
            .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("Failed to fetch release notes for v{}: HTTP {}", version, response.code());
                return;
            }

            JsonObject release = new JsonParser().parse(response.body().string()).getAsJsonObject();
            JsonElement notes = release.get("body");
            if (notes == null || notes.isJsonNull()) {
                return;
            }

            List<String> lines = formatReleaseNotes(notes.getAsString());
            if (lines.isEmpty()) {
                return;
            }

            displayInChat(version, lines);
        } catch (IOException e) {
            log.warn("Error fetching release notes for v{}", version, e);
        }
    }

    private void displayInChat(String version, List<String> lines) {
        clientThread.invoke(() -> {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                CHAT_PREFIX + "Updated to v" + version + " - here's what's new:", null);
            for (String line : lines) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", line, null);
            }
        });
    }

    private List<String> formatReleaseNotes(String body) {
        List<String> lines = new ArrayList<>();

        for (String rawLine : body.split("\\r?\\n")) {
            if (lines.size() >= MAX_CHANGELOG_LINES) {
                lines.add(" ...");
                break;
            }

            String line = rawLine.trim();
            if (line.startsWith("###")) {
                lines.add(stripMarkdown(line.replaceFirst("^#+\\s*", "")) + ":");
            } else if (line.startsWith("*") || line.startsWith("-")) {
                String text = stripMarkdown(line.replaceFirst("^[*-]\\s*", ""));
                if (!text.isEmpty()) {
                    lines.add(" - " + text);
                }
            }
        }

        return lines;
    }

    private String stripMarkdown(String text) {
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
        text = text.replaceAll("\\s*\\([0-9a-f]{7,40}\\)", "");
        text = text.replace("**", "").replace("`", "");
        return text.trim();
    }
}
