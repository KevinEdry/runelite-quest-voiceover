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
        downloadNotYetPresentDatabaseSource(okHttpClient);
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

    private static void downloadNotYetPresentDatabaseSource(OkHttpClient okHttpClient) {
        getFilesToDownload()
                .forEach(filename -> downloadFilename(okHttpClient, filename));
    }

    private static void downloadFilename(OkHttpClient okHttpClient, String filename) {
        if (RAW_GITHUB_DATABASE_BRANCH_URL == null) {
            // Hush intellij, it's okay, the potential NPE can't hurt you now
            log.error("Quest Voiceover plugin could not download database source due to an unexpected null RAW_GITHUB value");
            return;
        }
        HttpUrl databaseUrl = RAW_GITHUB_DATABASE_BRANCH_URL.newBuilder().addPathSegment(filename).build();
        Request request = new Request.Builder().url(databaseUrl).build();
        try (Response res = okHttpClient.newCall(request).execute()) {
            if (res.body() != null)
                Files.copy(new BufferedInputStream(res.body().byteStream()), DOWNLOAD_DIR.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Quest Voiceover plugin could not download database source.", e);
        }
    }

    private static Stream<String> getFilesToDownload() {
        Set<String> filesAlreadyPresent = getFilesPresent();

        return getUpdatedDatabase()
                .map(DatabaseSource::getResourceName)
                .filter(not(filesAlreadyPresent::contains));
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
