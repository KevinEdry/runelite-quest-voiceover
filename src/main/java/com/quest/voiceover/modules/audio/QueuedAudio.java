package com.quest.voiceover.modules.audio;

import lombok.Value;

@Value
public class QueuedAudio {
    String fileName;
    String questName;
    String characterName;
}
