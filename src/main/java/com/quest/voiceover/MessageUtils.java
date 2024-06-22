package com.quest.voiceover;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageUtils {

    String name;
    String text;
    String id;

    private String generateUniqueId(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageUtils(String message, String playerName) {
        String[] messageDeconstruction = message.split("\\|", 2);
        this.name = messageDeconstruction[0].equals(playerName) ? "Player" : messageDeconstruction[0];
        this.text = messageDeconstruction[1];
        this.id = this.generateUniqueId(String.format("%s|%s", this.name, this.text));
    }

}
