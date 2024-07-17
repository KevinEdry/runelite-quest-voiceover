package com.quest.voiceover.database;

import lombok.Getter;
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

    private static final String resourceName = "quest_voiceover_v1.db";

    private static final HttpUrl RAW_GITHUB_DATABASE_BRANCH_URL = HttpUrl.parse("https://github.com/KevinEdry/runelite-quest-voiceover/raw/database");
    private static final Path DOWNLOAD_DIR = Path.of(RuneLite.RUNELITE_DIR.getPath(), "quest-voiceover-database");

    private static final String VERSION_FILENAME = ".version";

    private static final Path VERSION_FILE = DOWNLOAD_DIR.resolve(VERSION_FILENAME);

    public static HttpUrl getResourceDownloadUrl() {
        assert RAW_GITHUB_DATABASE_BRANCH_URL != null;
        return RAW_GITHUB_DATABASE_BRANCH_URL.newBuilder().addPathSegment(resourceName).build();
    }

    public static Path getResourcePath() {
        return DOWNLOAD_DIR.resolve(resourceName);
    }

    private static void setResourceVersion(String version) {
        try {
            Files.createDirectories(VERSION_FILE.getParent());

            Files.writeString(VERSION_FILE, version,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            log.error("Failed to save database version.", e);
        }
    }

    public static void prepareDatabaseSource(OkHttpClient okHttpClient) {
        ensureDownloadDirectoryExists();
        downloadOrUpdateDatabase(okHttpClient);
    }

    private static String getDatabaseVersion() {
        try {
            if (Files.exists(VERSION_FILE)) {
                return Files.readString(VERSION_FILE).trim();
            }
        } catch (IOException e) {
            log.error("Failed to load database version.", e);
        }
        return "";
    }

    private static void updateDatabaseSource(InputStream data, String version) throws IOException {
        Path databasePath = getResourcePath();
        Files.copy(data, databasePath, StandardCopyOption.REPLACE_EXISTING);
        setResourceVersion(version);
    }

    public static String getDatabasePath() throws FileNotFoundException {
        Path databasePath = getResourcePath();
        if (!Files.exists(databasePath)) {
            throw new FileNotFoundException("Database file not found: " + databasePath);
        }
        return databasePath.toString();
    }

    private static void ensureDownloadDirectoryExists() {
        try {
            Files.createDirectories(getResourcePath().getParent());
        } catch (IOException e) {
            log.error("Could not create download directory", e);
        }
    }

    /**
     * We perform a HEAD request with the `If-None-Match` header to check if the etag has changed and the version with it.
     * If not, we get a `304 - Not Modified` response from the remote resource
     * If it did, we perform a GET request to get the actual data and persistently save the etag and the database on the local machine.
     */
    private static void downloadOrUpdateDatabase(OkHttpClient okHttpClient) {
        try {
            String resourceVersion = getDatabaseVersion();
            Request headRequest = new Request.Builder()
                    .url(getResourceDownloadUrl())
                    .header("If-None-Match", resourceVersion)
                    .head()
                    .build();

            try (Response headResponse = okHttpClient.newCall(headRequest).execute()) {
                if (headResponse.code() == 304) {
                    return;
                }

                String remoteEtag = headResponse.header("ETag");
                if (remoteEtag == null) {
                    log.warn("No ETag header is present in the remote response. {}", headResponse);
                    return;
                }

                if (!Files.exists(getResourcePath()) || !remoteEtag.equals(resourceVersion)) {
                    log.info("New database version found in remote: {}", remoteEtag);

                    // Perform GET request to download the file
                    Request getRequest = new Request.Builder().url(getResourceDownloadUrl()).build();
                    try (Response getResponse = okHttpClient.newCall(getRequest).execute()) {
                        if (!getResponse.isSuccessful()) {
                            log.error("Failed to download database: {}", getResponse.message());
                            return;
                        }

                        assert getResponse.body() != null;
                        try (InputStream in = getResponse.body().byteStream()) {
                            updateDatabaseSource(in, remoteEtag);
                        }

                        log.info("Database successfully updated!");
                    }
                }
            }
        } catch (IOException e) {
            log.error("Quest Voiceover plugin could not download or update database: {}", resourceName, e);
        } catch (NullPointerException e) {
            log.error("Null pointer exception occurred while downloading or updating database", e);
        }
    }
}