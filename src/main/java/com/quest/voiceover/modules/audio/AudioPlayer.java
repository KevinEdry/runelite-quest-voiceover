package com.quest.voiceover.modules.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Streaming MP3 player built on the JLayer decoder and a javax.sound line, replacing the
 * external jaco-mp3-player so all audio-device usage lives in the plugin's own reviewed code.
 */
@Slf4j
public class AudioPlayer {

    private static final int BITS_PER_SAMPLE = 16;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 100;
    private static final double AMPLITUDE_DECIBEL_FACTOR = 20.0;
    private static final int MICROS_PER_MILLI = 1000;
    private static final double LINE_BUFFER_SECONDS = 0.1;
    private static final int FADE_OUT_STEPS = 8;
    private static final long FADE_OUT_STEP_MILLIS = 2;
    private static final double NORMAL_SPEED = 1.0;
    private static final int DECODE_INITIAL_CAPACITY = 1 << 18;
    private static final int BUFFER_GROWTH_FACTOR = 2;
    private static final int WRITE_CHUNK_FRAMES = 1024;

    private final OkHttpClient okHttpClient;

    // Each play()/stop() bumps the generation; a worker whose generation is stale exits silently.
    private final AtomicInteger generation = new AtomicInteger();
    private final Object pauseLock = new Object();

    private volatile URL source;
    private volatile boolean active;
    private volatile boolean paused;
    private volatile boolean stopRequested;
    private volatile int volumePercent = MAX_VOLUME;
    private volatile double speed = NORMAL_SPEED;
    private volatile SourceDataLine line;

    public AudioPlayer(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public void setSpeed(double speed) {
        this.speed = speed <= 0 ? NORMAL_SPEED : speed;
    }

    public void add(URL url) {
        this.source = url;
    }

    public void clearPlayList() {
        this.source = null;
    }

    public synchronized void play() {
        if (active && paused) {
            resumeInternal();
            return;
        }

        URL current = source;
        if (current == null) {
            return;
        }

        signalStop();
        int currentGeneration = generation.incrementAndGet();
        stopRequested = false;
        paused = false;
        active = true;

        Thread worker = new Thread(() -> runPlayback(current, currentGeneration), "quest-voiceover-audio");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        signalStop();
        generation.incrementAndGet();
        active = false;
        paused = false;
    }

    public void pause() {
        if (!active || paused) {
            return;
        }
        paused = true;
    }

    public boolean isStopped() {
        return !active;
    }

    /**
     * Position of the current line in milliseconds, or 0 when nothing is playing. Holds steady
     * while paused because a starved line renders no further frames, letting callers such as the
     * speech highlighter follow the true audio rather than a wall-clock estimate.
     */
    public int getPositionMillis() {
        SourceDataLine currentLine = line;
        if (currentLine == null) {
            return 0;
        }
        return (int) (currentLine.getMicrosecondPosition() / MICROS_PER_MILLI);
    }

    public void setVolume(int volume) {
        volumePercent = clampVolume(volume);
        SourceDataLine currentLine = line;
        if (currentLine != null) {
            applyGain(currentLine, volumePercent);
        }
    }

    private void resumeInternal() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    private void signalStop() {
        // Only flags are touched here; the worker owns the line and reacts between frames,
        // so there is no cross-thread contention on the audio device.
        stopRequested = true;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    private void runPlayback(URL url, int myGeneration) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("Failed to fetch audio {}: HTTP {}", url, response.code());
                return;
            }
            playFromStream(response.body().byteStream(), myGeneration);
        } catch (Exception exception) {
            log.error("Failed to play audio: {}", url, exception);
        }
    }

    private void playFromStream(InputStream rawInput, int myGeneration) throws Exception {
        double currentSpeed = speed;
        PlaybackResult result = PlaybackResult.NONE;

        try (InputStream input = new BufferedInputStream(rawInput)) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();

            result = currentSpeed == NORMAL_SPEED
                ? playStreaming(bitstream, decoder, myGeneration)
                : playStretched(bitstream, decoder, myGeneration, currentSpeed);

            bitstream.close();
        } finally {
            finishLine(result.line, result.playedToEnd && isCurrent(myGeneration));
            if (isCurrent(myGeneration)) {
                active = false;
            }
        }
    }

    private PlaybackResult playStreaming(Bitstream bitstream, Decoder decoder, int myGeneration) throws Exception {
        SourceDataLine localLine = null;

        while (isCurrent(myGeneration)) {
            Header header = bitstream.readFrame();
            if (header == null) {
                break;
            }

            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);

            if (localLine == null) {
                localLine = startLine(samples.getSampleFrequency(), samples.getChannelCount());
            }

            waitWhilePaused(myGeneration);
            if (!isCurrent(myGeneration)) {
                break;
            }

            byte[] pcm = toLittleEndianBytes(samples.getBuffer(), 0, samples.getBufferLength());
            localLine.write(pcm, 0, pcm.length);
            bitstream.closeFrame();
        }

        return new PlaybackResult(localLine, localLine != null && isCurrent(myGeneration));
    }

    private PlaybackResult playStretched(Bitstream bitstream, Decoder decoder, int myGeneration, double currentSpeed) throws Exception {
        DecodedAudio decoded = decodeAll(bitstream, decoder, myGeneration);
        if (decoded == null || !isCurrent(myGeneration)) {
            return PlaybackResult.NONE;
        }

        short[] stretched = TimeStretcher.stretch(
            decoded.samples, decoded.sampleCount, decoded.channels, currentSpeed);
        SourceDataLine localLine = startLine(decoded.sampleRate, decoded.channels);
        boolean playedToEnd = writeSamples(localLine, stretched, decoded.channels, myGeneration);
        return new PlaybackResult(localLine, playedToEnd);
    }

    private DecodedAudio decodeAll(Bitstream bitstream, Decoder decoder, int myGeneration) throws Exception {
        short[] samples = new short[DECODE_INITIAL_CAPACITY];
        int sampleCount = 0;
        int sampleRate = 0;
        int channels = 0;

        while (isCurrent(myGeneration)) {
            Header header = bitstream.readFrame();
            if (header == null) {
                break;
            }

            SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            if (sampleRate == 0) {
                sampleRate = buffer.getSampleFrequency();
                channels = buffer.getChannelCount();
            }

            int length = buffer.getBufferLength();
            if (sampleCount + length > samples.length) {
                samples = Arrays.copyOf(samples, Math.max(samples.length * BUFFER_GROWTH_FACTOR, sampleCount + length));
            }
            System.arraycopy(buffer.getBuffer(), 0, samples, sampleCount, length);
            sampleCount += length;
            bitstream.closeFrame();
        }

        if (sampleRate == 0) {
            return null;
        }
        return new DecodedAudio(samples, sampleCount, sampleRate, channels);
    }

    private boolean writeSamples(SourceDataLine localLine, short[] samples, int channels, int myGeneration) {
        int chunkSamples = WRITE_CHUNK_FRAMES * channels;
        int offset = 0;

        while (offset < samples.length) {
            waitWhilePaused(myGeneration);
            if (!isCurrent(myGeneration)) {
                return false;
            }

            int length = Math.min(chunkSamples, samples.length - offset);
            byte[] pcm = toLittleEndianBytes(samples, offset, length);
            localLine.write(pcm, 0, pcm.length);
            offset += length;
        }

        return true;
    }

    private SourceDataLine startLine(int sampleRate, int channels) throws Exception {
        SourceDataLine sourceLine = openLine(sampleRate, channels);
        line = sourceLine;
        applyGain(sourceLine, volumePercent);
        sourceLine.start();
        return sourceLine;
    }

    private void finishLine(SourceDataLine localLine, boolean playedToEnd) {
        if (localLine == null) {
            return;
        }
        if (playedToEnd) {
            localLine.drain();
        } else {
            // Interrupted mid-line: fade the buffered tail to silence so the cut is click-free.
            fadeOut(localLine);
            localLine.flush();
        }
        localLine.stop();
        localLine.close();
        if (line == localLine) {
            line = null;
        }
    }

    private void fadeOut(SourceDataLine sourceLine) {
        if (!sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gainControl = (FloatControl) sourceLine.getControl(FloatControl.Type.MASTER_GAIN);
        float startGain = gainControl.getValue();
        float minGain = gainControl.getMinimum();

        for (int step = 1; step <= FADE_OUT_STEPS; step++) {
            float value = startGain + (minGain - startGain) * step / FADE_OUT_STEPS;
            gainControl.setValue(Math.max(minGain, Math.min(gainControl.getMaximum(), value)));
            try {
                Thread.sleep(FADE_OUT_STEP_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean isCurrent(int myGeneration) {
        return !stopRequested && myGeneration == generation.get();
    }

    private void waitWhilePaused(int myGeneration) {
        synchronized (pauseLock) {
            while (paused && !stopRequested && myGeneration == generation.get()) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private SourceDataLine openLine(int sampleRate, int channels) throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, BITS_PER_SAMPLE, channels, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
        int bufferBytes = (int) (format.getFrameSize() * format.getFrameRate() * LINE_BUFFER_SECONDS);
        sourceLine.open(format, bufferBytes);
        return sourceLine;
    }

    private void applyGain(SourceDataLine sourceLine, int volume) {
        if (!sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gainControl = (FloatControl) sourceLine.getControl(FloatControl.Type.MASTER_GAIN);
        float gainDecibels = volume <= MIN_VOLUME
            ? gainControl.getMinimum()
            : (float) (AMPLITUDE_DECIBEL_FACTOR * Math.log10((double) volume / MAX_VOLUME));
        float clamped = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), gainDecibels));
        gainControl.setValue(clamped);
    }

    private byte[] toLittleEndianBytes(short[] samples, int offset, int length) {
        ByteBuffer bytes = ByteBuffer.allocate(length * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bytes.asShortBuffer().put(samples, offset, length);
        return bytes.array();
    }

    private int clampVolume(int volume) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }

    private static final class PlaybackResult {
        private static final PlaybackResult NONE = new PlaybackResult(null, false);

        private final SourceDataLine line;
        private final boolean playedToEnd;

        private PlaybackResult(SourceDataLine line, boolean playedToEnd) {
            this.line = line;
            this.playedToEnd = playedToEnd;
        }
    }

    private static final class DecodedAudio {
        private final short[] samples;
        private final int sampleCount;
        private final int sampleRate;
        private final int channels;

        private DecodedAudio(short[] samples, int sampleCount, int sampleRate, int channels) {
            this.samples = samples;
            this.sampleCount = sampleCount;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }
    }
}
