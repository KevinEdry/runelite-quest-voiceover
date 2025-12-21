package com.quest.voiceover.utility;

public class LevenshteinUtility {

    public static int distance(String source, String target) {
        if (source.length() < target.length()) {
            String swap = source;
            source = target;
            target = swap;
        }

        int[] previousRow = new int[target.length() + 1];
        int[] currentRow = new int[target.length() + 1];

        for (int targetIndex = 0; targetIndex <= target.length(); targetIndex++) {
            previousRow[targetIndex] = targetIndex;
        }

        for (int sourceIndex = 1; sourceIndex <= source.length(); sourceIndex++) {
            currentRow[0] = sourceIndex;
            for (int targetIndex = 1; targetIndex <= target.length(); targetIndex++) {
                int cost = source.charAt(sourceIndex - 1) == target.charAt(targetIndex - 1) ? 0 : 1;
                currentRow[targetIndex] = Math.min(
                    Math.min(previousRow[targetIndex] + 1, currentRow[targetIndex - 1] + 1),
                    previousRow[targetIndex - 1] + cost
                );
            }
            int[] swap = previousRow;
            previousRow = currentRow;
            currentRow = swap;
        }

        return previousRow[target.length()];
    }

    public static double similarity(String source, String target) {
        int distance = distance(source, target);
        int maxLength = Math.max(source.length(), target.length());
        return maxLength == 0 ? 1.0 : 1.0 - ((double) distance / maxLength);
    }
}
