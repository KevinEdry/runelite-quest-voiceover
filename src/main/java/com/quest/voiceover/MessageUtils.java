package com.quest.voiceover;

import javax.inject.Inject;

public class MessageUtils {

    private static final String PLAYER = "Player";

    @Inject
    private HashUtil hashUtil;

    String name;
    String text;
    String id;

    public MessageUtils(String message, String playerName) {
        String[] messageArray = message.split("\\|", 2);
        this.name = messageArray[0].equals(playerName) ? PLAYER : messageArray[0];
        this.text = messageArray[1].trim();
        this.id = HashUtil.convertToMD5(this.name + "|" + this.text);
    }
}
