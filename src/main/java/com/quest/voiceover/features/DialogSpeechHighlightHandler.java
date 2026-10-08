package com.quest.voiceover.features;

import com.quest.voiceover.QuestVoiceoverConfig;
import com.quest.voiceover.modules.audio.AudioManager;
import com.quest.voiceover.modules.dialog.DialogManager;
import com.quest.voiceover.utility.ColorUtility;
import com.quest.voiceover.Constants;
import com.quest.voiceover.utility.TextUtility;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class DialogSpeechHighlightHandler {

    private static final double FALLBACK_WORDS_PER_SECOND = 2.5;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\S+");
    private static final Pattern PUNCTUATION_PAUSE_PATTERN = Pattern.compile("[.!?]+$");
    private static final int PUNCTUATION_PAUSE_WEIGHT = 7;
    private static final long HIGHLIGHT_POLL_MS = 50;

    @Inject
    private QuestVoiceoverConfig config;

    @Inject
    private DialogManager dialogManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private AudioManager audioManager;

    private String originalText;
    private List<WordPosition> wordPositions;
    private volatile int currentWordIndex;
    private long playbackStartWallClock;
    private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();

    public void startAsync(String audioUri, String dialogText) {
        if (!config.speechHighlighting()) {
            return;
        }

        executor.submit(() -> {
            long durationMs = estimateDurationMs(audioUri);
            start(dialogText, durationMs);
        });
    }

    private long estimateDurationMs(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }

        HttpUrl url = Constants.RAW_GITHUB_SOUND_BRANCH_URL.newBuilder()
            .addPathSegment(fileName)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .head()
            .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String contentLength = response.header("Content-Length");
            if (contentLength != null) {
                long fileBytes = Long.parseLong(contentLength);
                long bytesPerSecond = (Constants.MP3_BITRATE_KBPS * 1000L) / 8;
                return (fileBytes * 1000) / bytesPerSecond;
            }
        } catch (IOException | NumberFormatException e) {
            log.warn("Failed to estimate audio duration for {}", fileName, e);
        }

        return 0;
    }

    private void start(String dialogText, long durationMs) {
        stop();

        originalText = dialogText;
        wordPositions = parseWordPositions(dialogText);
        currentWordIndex = 0;

        if (wordPositions.isEmpty()) {
            return;
        }

        long effectiveDuration = durationMs > 0 ? durationMs : calculateFallbackDuration();
        scheduleWordHighlights(effectiveDuration);
    }

    public void stop() {
        for (ScheduledFuture<?> task : scheduledTasks) {
            task.cancel(false);
        }
        reset();
    }

    private void reset() {
        scheduledTasks.clear();
        originalText = null;
        wordPositions = null;
        currentWordIndex = 0;
    }

    private List<WordPosition> parseWordPositions(String text) {
        List<WordPosition> positions = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String word = matcher.group();
            String cleanWord = word.replaceAll("<[^>]+>", "");
            int weight = cleanWord.length();

            if (PUNCTUATION_PAUSE_PATTERN.matcher(cleanWord).find()) {
                weight += PUNCTUATION_PAUSE_WEIGHT;
            }

            positions.add(new WordPosition(matcher.start(), matcher.end(), weight));
        }

        return positions;
    }

    private void scheduleWordHighlights(long totalDurationMs) {
        long[] wordStartTimes = computeWordStartTimes(totalDurationMs);
        playbackStartWallClock = System.currentTimeMillis();
        pollHighlight(wordStartTimes);
    }

    private long[] computeWordStartTimes(long totalDurationMs) {
        int totalWeight = calculateTotalWeight();
        long[] startTimes = new long[wordPositions.size()];
        long cumulativeTime = 0;

        for (int wordIndex = 0; wordIndex < wordPositions.size(); wordIndex++) {
            startTimes[wordIndex] = cumulativeTime;
            cumulativeTime += calculateWordDuration(wordPositions.get(wordIndex).weight, totalWeight, totalDurationMs);
        }

        return startTimes;
    }

    private void pollHighlight(long[] wordStartTimes) {
        ScheduledFuture<?> task = executor.schedule(() -> {
            long position = resolvePlaybackPosition();
            int wordsStarted = countWordsStarted(wordStartTimes, position);

            if (wordsStarted > currentWordIndex) {
                highlightUpToWord(wordsStarted);
            }

            if (currentWordIndex < wordStartTimes.length) {
                pollHighlight(wordStartTimes);
            }
        }, HIGHLIGHT_POLL_MS, TimeUnit.MILLISECONDS);
        scheduledTasks.add(task);
    }

    /**
     * Prefer the player's true position so highlighting tracks the audio (and halts with it while
     * paused); fall back to elapsed wall-clock before the line opens or once playback has ended.
     */
    private long resolvePlaybackPosition() {
        // Word timings are computed against the un-sped audio, so scale elapsed playback back
        // onto that timeline when the user speeds voiceovers up.
        double speed = config.playbackSpeed().getMultiplier();
        long audioPosition = audioManager.getPlaybackPositionMillis();
        if (audioPosition > 0) {
            return (long) (audioPosition * speed);
        }
        return (long) ((System.currentTimeMillis() - playbackStartWallClock) * speed);
    }

    private int countWordsStarted(long[] wordStartTimes, long position) {
        int count = 0;
        while (count < wordStartTimes.length && wordStartTimes[count] <= position) {
            count++;
        }
        return count;
    }

    private void highlightUpToWord(int wordIndex) {
        if (wordPositions == null || originalText == null) {
            return;
        }

        currentWordIndex = wordIndex;
        String highlightedText = buildHighlightedText();
        clientThread.invokeLater(() -> dialogManager.setDialogText(highlightedText));
    }

    private int calculateTotalWeight() {
        int total = 0;
        for (WordPosition pos : wordPositions) {
            total += pos.weight;
        }
        return Math.max(total, 1);
    }

    private long calculateWordDuration(int weight, int totalWeight, long totalDurationMs) {
        double proportion = (double) weight / totalWeight;
        return (long) (proportion * totalDurationMs);
    }

    private long calculateFallbackDuration() {
        return (long) ((wordPositions.size() / FALLBACK_WORDS_PER_SECOND) * 1000);
    }

    private String buildHighlightedText() {
        if (currentWordIndex <= 0 || wordPositions.isEmpty()) {
            return originalText;
        }

        String colorTag = ColorUtility.toOsrsColorTag(config.speechHighlightColor());
        int highlightEnd = wordPositions.get(Math.min(currentWordIndex, wordPositions.size()) - 1).end;

        String highlightedPortion = originalText.substring(0, highlightEnd);
        String remainingPortion = originalText.substring(highlightEnd);

        highlightedPortion = TextUtility.reapplyColorAfterLineBreaks(highlightedPortion, colorTag);

        return colorTag + highlightedPortion + "</col>" + remainingPortion;
    }

    private static class WordPosition {
        final int start;
        final int end;
        final int weight;

        WordPosition(int start, int end, int weight) {
            this.start = start;
            this.end = end;
            this.weight = weight;
        }
    }
}
