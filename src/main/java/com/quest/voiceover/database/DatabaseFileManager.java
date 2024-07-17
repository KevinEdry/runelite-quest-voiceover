package com.quest.voiceover.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

@Slf4j
public class DatabaseFileManager {
    private DatabaseFileManager() {}

    private static final Path DOWNLOAD_DIR = Path.of(RuneLite.RUNELITE_DIR.getPath(), "quest-voiceover-database");
    private static final String DELETE_WARNING_FILENAME = "_EXTRA_FILES_WILL_BE_DELETED_BUT_FOLDERS_WILL_REMAIN";
    private static final Path DELETE_WARNING_FILE = DOWNLOAD_DIR.resolve(DELETE_WARNING_FILENAME);
    public static final HttpUrl RAW_GITHUB_DATABASE_BRANCH_URL = HttpUrl.parse("https://github.com/KevinEdry/runelite-quest-voiceover/raw/database");

    public static String getDatabaseSourcePath(DatabaseSource databaseSource) throws FileNotFoundException {
        return Path.of(DOWNLOAD_DIR.resolve(databaseSource.getResourceName()).toUri()).toString();
    }

    public static void prepareDatabaseSource(OkHttpClient okHttpClient) {
        log.info(DOWNLOAD_DIR.toString());
        ensureDownloadDirectoryExists();
        deleteUndesiredFilesIgnoringFolders();
        downloadOrUpdateDatabaseSources(okHttpClient);
    }

    private static void ensureDownloadDirectoryExists() {
        try {
            if (!Files.exists(DOWNLOAD_DIR))
                Files.createDirectories(DOWNLOAD_DIR);
            Files.createFile(DELETE_WARNING_FILE);
        } catch (FileAlreadyExistsException ignored) {
            /* ignored */
        } catch (IOException e) {
            log.error("Could not create download directory or warning file", e);
        }
    }

    private static void deleteUndesiredFilesIgnoringFolders() {
        Set<String> desiredDatabaseFileName = getUpdatedDatabase()
                .map(DatabaseSource::getResourceName)
                .collect(Collectors.toSet());

        Set<Path> toDelete = getFilesPresent().stream()
                .filter(not(desiredDatabaseFileName::contains))
                .map(DOWNLOAD_DIR::resolve)
                .collect(Collectors.toSet());
        try {
            for (Path pathToDelete : toDelete) {
                Files.delete(pathToDelete);
            }
        } catch (IOException e) {
            log.error("Failed to delete disused database source file", e);
        }
    }

    private static void downloadOrUpdateDatabaseSources(OkHttpClient okHttpClient) {
        getUpdatedDatabase()
                .forEach(databaseSource -> downloadOrUpdateFile(okHttpClient, databaseSource.getResourceName()));
    }

    private static void downloadOrUpdateFile(OkHttpClient okHttpClient, String filename) {
        if (RAW_GITHUB_DATABASE_BRANCH_URL == null) {
            log.error("Quest Voiceover plugin could not download database source due to an unexpected null RAW_GITHUB value");
            return;
        }
        HttpUrl databaseUrl = RAW_GITHUB_DATABASE_BRANCH_URL.newBuilder().addPathSegment(filename).build();

        try {
            Path localFile = DOWNLOAD_DIR.resolve(filename);
            long localFileSize = Files.exists(localFile) ? Files.size(localFile) : -1;

            // Perform HEAD request to get file size
            Request headRequest = new Request.Builder().url(databaseUrl).head().build();
            try (Response headResponse = okHttpClient.newCall(headRequest).execute()) {
                if (!headResponse.isSuccessful()) {
                    log.error("Failed to get file metadata: {}", filename);
                    return;
                }

                long remoteFileSize = headResponse.header("Content-Length") != null
                        ? Long.parseLong(Objects.requireNonNull(headResponse.header("Content-Length")))
                        : -1;

                log.info("remote file size: {}", remoteFileSize);
                log.info("local file size: {}", localFileSize);
                if (localFileSize == -1 || localFileSize != remoteFileSize) {
                    if (localFileSize == -1) {
                        log.info("Downloading new file: {}", filename);
                    } else {
                        log.info("Updating file due to size difference: {}", filename);
                    }

                    // Perform GET request to download the file
                    Request getRequest = new Request.Builder().url(databaseUrl).build();
                    try (Response getResponse = okHttpClient.newCall(getRequest).execute()) {
                        if (!getResponse.isSuccessful()) {
                            log.error("Failed to download file: {}", filename);
                            return;
                        }

                        assert getResponse.body() != null;
                        try (InputStream in = getResponse.body().byteStream()) {
                            Files.copy(in, localFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    log.info("File is up to date: {}", filename);
                }
            }
        } catch (IOException e) {
            log.error("Quest Voiceover plugin could not download or update database source: {}", filename, e);
        }
    }

    private static Set<String> getFilesPresent() {
        try (Stream<Path> paths = Files.list(DOWNLOAD_DIR)) {
            return paths
                    .filter(path -> !Files.isDirectory(path))
                    .map(Path::toFile)
                    .map(File::getName)
                    .filter(filename -> !DELETE_WARNING_FILENAME.equals(filename))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.warn("Could not list files in {}, assuming it to be empty", DOWNLOAD_DIR);
            return Set.of();
        }
    }

    private static Stream<DatabaseSource> getUpdatedDatabase() {
        return Arrays.stream(DatabaseSource.values());
    }
}