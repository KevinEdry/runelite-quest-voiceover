package com.quest.voiceover.modules.audio;

public final class TimeStretcher {

    private static final double NORMAL_SPEED = 1.0;
    private static final int WINDOW_FRAMES = 1024;
    private static final int SYNTHESIS_HOP = WINDOW_FRAMES / 2;
    private static final int MIN_ANALYSIS_HOP = 1;
    private static final int SEARCH_FRAMES = 256;
    private static final int CORRELATION_STRIDE = 4;
    private static final double CORRELATION_EPSILON = 1.0;
    private static final int MIN_STRETCH_FRAMES = WINDOW_FRAMES * 2;
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double HANN_AMPLITUDE = 0.5;
    private static final short SHORT_MAX = Short.MAX_VALUE;
    private static final short SHORT_MIN = Short.MIN_VALUE;
    private static final float[] HANN_WINDOW = hannWindow();

    private TimeStretcher() {}

    public static short[] stretch(short[] samples, int sampleCount, int channels, double speed) {
        int totalFrames = sampleCount / channels;
        if (speed == NORMAL_SPEED || totalFrames < MIN_STRETCH_FRAMES) {
            return trim(samples, sampleCount);
        }

        int analysisHop = Math.max(MIN_ANALYSIS_HOP, (int) Math.round(SYNTHESIS_HOP * speed));
        int maxGrains = (totalFrames - WINDOW_FRAMES) / analysisHop;
        float[] output = new float[((maxGrains + 1) * SYNTHESIS_HOP + WINDOW_FRAMES) * channels];

        overlapAdd(output, 0, samples, 0, channels);
        int naturalStart = SYNTHESIS_HOP;
        int lastOutputFrameEnd = WINDOW_FRAMES;
        int lastGrainStart = 0;

        for (int grain = 1; grain <= maxGrains; grain++) {
            int analysisStart = grain * analysisHop;
            int grainStart = analysisStart + findBestDelta(samples, channels, totalFrames, analysisStart, naturalStart);
            if (grainStart + WINDOW_FRAMES > totalFrames || grainStart < 0) {
                break;
            }

            int outputStart = grain * SYNTHESIS_HOP;
            overlapAdd(output, outputStart, samples, grainStart, channels);
            lastOutputFrameEnd = outputStart + WINDOW_FRAMES;
            lastGrainStart = grainStart;
            naturalStart = grainStart + SYNTHESIS_HOP;
        }

        int tailGrainStart = totalFrames - WINDOW_FRAMES;
        if (tailGrainStart > lastGrainStart) {
            int outputStart = lastOutputFrameEnd - SYNTHESIS_HOP;
            overlapAdd(output, outputStart, samples, tailGrainStart, channels);
            lastOutputFrameEnd = outputStart + WINDOW_FRAMES;
        }

        return toShorts(output, lastOutputFrameEnd * channels);
    }

    private static int findBestDelta(short[] samples, int channels, int totalFrames,
                                     int analysisStart, int naturalStart) {
        if (naturalStart < 0 || naturalStart + WINDOW_FRAMES > totalFrames) {
            return 0;
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        int bestDelta = 0;

        for (int delta = -SEARCH_FRAMES; delta <= SEARCH_FRAMES; delta++) {
            int candidate = analysisStart + delta;
            if (candidate < 0 || candidate + WINDOW_FRAMES > totalFrames) {
                continue;
            }

            double dot = 0;
            double energy = 0;
            for (int frame = 0; frame < WINDOW_FRAMES; frame += CORRELATION_STRIDE) {
                double candidateValue = frameValue(samples, candidate + frame, channels);
                double naturalValue = frameValue(samples, naturalStart + frame, channels);
                dot += candidateValue * naturalValue;
                energy += candidateValue * candidateValue;
            }

            double score = dot / Math.sqrt(energy + CORRELATION_EPSILON);
            if (score > bestScore) {
                bestScore = score;
                bestDelta = delta;
            }
        }

        return bestDelta;
    }

    private static void overlapAdd(float[] output, int outputFrameStart, short[] samples,
                                   int inputFrameStart, int channels) {
        for (int frame = 0; frame < WINDOW_FRAMES; frame++) {
            float weight = HANN_WINDOW[frame];
            int outBase = (outputFrameStart + frame) * channels;
            int inBase = (inputFrameStart + frame) * channels;
            for (int channel = 0; channel < channels; channel++) {
                output[outBase + channel] += weight * samples[inBase + channel];
            }
        }
    }

    private static float frameValue(short[] samples, int frame, int channels) {
        int base = frame * channels;
        int sum = 0;
        for (int channel = 0; channel < channels; channel++) {
            sum += samples[base + channel];
        }
        return (float) sum / channels;
    }

    private static float[] hannWindow() {
        float[] weights = new float[WINDOW_FRAMES];
        for (int i = 0; i < WINDOW_FRAMES; i++) {
            weights[i] = (float) (HANN_AMPLITUDE * (1.0 - Math.cos(TWO_PI * i / (WINDOW_FRAMES - 1))));
        }
        return weights;
    }

    private static short[] toShorts(float[] output, int length) {
        short[] result = new short[length];
        for (int i = 0; i < length; i++) {
            float value = output[i];
            if (value > SHORT_MAX) {
                result[i] = SHORT_MAX;
            } else if (value < SHORT_MIN) {
                result[i] = SHORT_MIN;
            } else {
                result[i] = (short) value;
            }
        }
        return result;
    }

    private static short[] trim(short[] samples, int sampleCount) {
        if (sampleCount == samples.length) {
            return samples;
        }
        short[] result = new short[sampleCount];
        System.arraycopy(samples, 0, result, 0, sampleCount);
        return result;
    }
}
