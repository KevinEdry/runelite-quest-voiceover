package com.quest.voiceover.utility;

public final class MessageUtility {

    private MessageUtility() {}

    public static final class ParsedMessage {
        private final String characterName;
        private final String dialogText;
        private final String messageId;

        private ParsedMessage(String characterName, String dialogText, String messageId) {
            this.characterName = characterName;
            this.dialogText = dialogText;
            this.messageId = messageId;
        }

        public String characterName() {
            return characterName;
        }

        public String dialogText() {
            return dialogText;
        }

        public String messageId() {
            return messageId;
        }
    }

    public static ParsedMessage parseRawMessage(String rawMessage, String playerName, String playerVoiceName) {
        String[] parts = rawMessage.split("\\|", 2);

        String characterName = normalizeCharacterName(parts[0], playerName, playerVoiceName);
        String dialogText = parts[1].trim();
        String messageId = HashUtility.toMD5(characterName + "|" + dialogText);

        return new ParsedMessage(characterName, dialogText, messageId);
    }

    public static String cleanWidgetText(String text, String playerName) {
        String cleaned = TextUtility.cleanForMatching(text);

        if (playerName != null && !playerName.isEmpty()) {
            cleaned = cleaned.replace(playerName, "");
        }

        return cleaned;
    }

    private static String normalizeCharacterName(String name, String playerName, String playerVoiceName) {
        return name.equals(playerName) ? playerVoiceName : name;
    }
}
