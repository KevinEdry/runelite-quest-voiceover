package com.voiceover;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageUtils {

    private static final String PLAYER = "Player";

    @Inject
    private HashUtils hashUtils;

    String name;
    String text;
    String id;

    public MessageUtils(String message, String playerName) {
        String[] messageArray = message.split("\\|", 2);
        this.name = messageArray[0].equals(playerName) ? PLAYER : messageArray[0];
        this.text = messageArray[1];
        this.id = HashUtils.convertToSHA(this.name + "|" + this.text);
    }

}
