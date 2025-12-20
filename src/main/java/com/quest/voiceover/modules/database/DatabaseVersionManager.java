package com.quest.voiceover.modules.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Slf4j
public class DatabaseVersionManager {

    private static final String DATABASE_FILENAME = "quest_voiceover.db";
    private static final String VERSION_FILENAME = ".version";

    private static final HttpUrl RAW_GITHUB_DATABASE_URL =
        HttpUrl.parse("https://github.com/KevinEdry/runelite-quest-voiceover/raw/database");

    private static final Path DOWNLOAD_DIR =
        Path.of(RuneLite.RUNELITE_DIR.getPath(), "quest-voiceover");

    private static final Path VERSION_FILE = DOWNLOAD_DIR.resolve(VERSION_FILENAME);
    private static final Path DATABASE_FILE = DOWNLOAD_DIR.resolve(DATABASE_FILENAME);

    public static void prepareDatabaseSource(OkHttpClient okHttpClient) {
        ensureDownloadDirectoryExists();
        downloadOrUpdateDatabase(okHttpClient);
    }

    public static String getDatabasePath() throws FileNotFoundException {
        if (!Files.exists(DATABASE_FILE)) {
            throw new FileNotFoundException("Database file not found: " + DATABASE_FILE);
        }
        return DATABASE_FILE.toString();
    }

    private static void ensureDownloadDirectoryExists() {
        try {
            Files.createDirectories(DOWNLOAD_DIR);
        } catch (IOException e) {
            log.error("Failed to create download directory", e);
        }
    }

    private static void downloadOrUpdateDatabase(OkHttpClient okHttpClient) {
        try {
            String currentVersion = readVersionFile();
            HttpUrl downloadUrl = buildDownloadUrl();

            Request headRequest = new Request.Builder()
                .url(downloadUrl)
                .header("If-None-Match", currentVersion)
                .head()
                .build();

            try (Response headResponse = okHttpClient.newCall(headRequest).execute()) {
                if (headResponse.code() == 304) {
                    return;
                }

                String remoteEtag = headResponse.header("ETag");
                if (remoteEtag == null) {
                    log.warn("No ETag header in remote response");
                    return;
                }

                if (shouldDownload(remoteEtag, currentVersion)) {
                    downloadDatabase(okHttpClient, downloadUrl, remoteEtag);
                }
            }
        } catch (IOException e) {
            log.error("Failed to download or update database", e);
        }
    }

    private static boolean shouldDownload(String remoteEtag, String currentVersion) {
        return !Files.exists(DATABASE_FILE) || !remoteEtag.equals(currentVersion);
    }

    private static void downloadDatabase(OkHttpClient client, HttpUrl url, String version) throws IOException {
        log.info("Downloading new database version: {}", version);

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to download database: {}", response.message());
                return;
            }

            try (InputStream inputStream = response.body().byteStream()) {
                Files.copy(inputStream, DATABASE_FILE, StandardCopyOption.REPLACE_EXISTING);
                writeVersionFile(version);
                log.info("Database updated successfully");
            }
        }
    }

    private static HttpUrl buildDownloadUrl() {
        return RAW_GITHUB_DATABASE_URL.newBuilder()
            .addPathSegment(DATABASE_FILENAME)
            .build();
    }

    private static String readVersionFile() {
        try {
            if (Files.exists(VERSION_FILE)) {
                return Files.readString(VERSION_FILE).trim();
            }
        } catch (IOException e) {
            log.error("Failed to read version file", e);
        }
        return "";
    }

    private static void writeVersionFile(String version) {
        try {
            Files.writeString(VERSION_FILE, version,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write version file", e);
        }
    }
}
