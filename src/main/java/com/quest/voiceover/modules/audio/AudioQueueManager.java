package com.quest.voiceover.modules.audio;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Singleton
public class AudioQueueManager {

    private final Queue<QueuedAudio> queue = new ConcurrentLinkedQueue<>();

    @Getter
    private QueuedAudio currentlyPlaying;

    public void add(String fileName, String questName, String characterName) {
        QueuedAudio item = new QueuedAudio(fileName, questName, characterName);
        queue.add(item);
        log.info("Queued audio: {} - {} (queue size: {})", characterName, questName, queue.size());
    }

    public QueuedAudio poll() {
        currentlyPlaying = queue.poll();
        return currentlyPlaying;
    }

    public void setCurrentlyPlaying(String fileName, String questName, String characterName) {
        currentlyPlaying = new QueuedAudio(fileName, questName, characterName);
    }

    public void clear() {
        queue.clear();
        currentlyPlaying = null;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
