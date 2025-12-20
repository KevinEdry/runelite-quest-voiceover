package com.quest.voiceover.utility;

public class MessageParser {

    private static final String PLAYER_NAME_PLACEHOLDER = "Player";

    private final String characterName;
    private final String dialogText;
    private final String messageId;

    public MessageParser(String rawMessage, String playerName) {
        String[] parts = rawMessage.split("\\|", 2);

        this.characterName = normalizeCharacterName(parts[0], playerName);
        this.dialogText = parts[1].trim();
        this.messageId = HashUtil.toMD5(this.characterName + "|" + this.dialogText);
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getDialogText() {
        return dialogText;
    }

    public String getMessageId() {
        return messageId;
    }

    private String normalizeCharacterName(String name, String playerName) {
        return name.equals(playerName) ? PLAYER_NAME_PLACEHOLDER : name;
    }
}
