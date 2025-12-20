# Extract Quest Transcript

Extract dialogue and character data from an OSRS Wiki quest transcript page.

**Input:** $ARGUMENTS (OSRS Wiki quest transcript URL, e.g., https://oldschool.runescape.wiki/w/Transcript:Cook%27s_Assistant)

## Instructions

1. **Fetch the wiki page** at the provided URL using a curl get request

2. **Extract the quest name** from the page title (remove "Transcript:" prefix)

3. **Extract all dialogue lines** with their speaker names:
   - Include the "Player" as a valid speaker
   - Skip menu options (player choices shown as clickable options)
   - Skip item descriptions and examine text
   - Skip stage directions (text in parentheses describing actions)
   - Skip section headers and navigation elements
   - **NEVER merge lines together** - each `<b>Speaker:</b>` tag on the wiki represents exactly one dialogue box as shown in-game, preserve this 1:1 mapping
   - Deduplicate exact repeated lines (same speaker + same text)

4. **Resolve dynamic text:**
   - Remove bracketed variables like `[1-19]`, `[item]`, `[number]`, `[Player name]`
   - Keep the sentence grammatically correct after removal
   - Examples:
     - "I need [1-19] more eggs" → "I need more eggs"
     - "Hello [Player name], welcome!" → "Hello, welcome!"
     - "Bring me [number] items" → "Bring me items"

5. **Extract unique characters** (exclude "Player"):
   - For each character, search the OSRS Wiki for more information about them
   - Create an ElevenLabs voice description in this format: age, gender, accent, tone, personality
   - Keep descriptions to 1-2 sentences, max 500 characters
   - Base the description on the character's lore, appearance, and role in the game
   - **Never use the word "child"** in descriptions (ElevenLabs blocks it) - use alternatives like "young", "youthful", or specific ages instead

6. **Generate a branch name** for GitHub:
   - Format: `quest/{quest-name-lowercase-hyphenated}`
   - Example: "Cook's Assistant" → `quest/cooks-assistant`

7. **Output a JSON file** saved to `transcripts/{quest-name-slug}.json` with this structure:

8. **Validation** verify transcript lines:
   - Read the json we just generated and count the amount of "lines".
   - Compare that with the wiki page to make sure the amount is correct.

```json
{
  "branch": "quest/quest-name",
  "quest_name": "Quest Name",
  "lines": [
    { "character": "NPC Name", "line": "Dialogue text here." },
    { "character": "Player", "line": "Player's response." }
  ],
  "characters": [
    {
      "name": "NPC Name",
      "description": "Middle-aged male, British accent, gruff and impatient tone, a hardworking cook who takes pride in his work but is easily stressed."
    }
  ]
}
```

## Important Notes

- Dialogue lines should preserve the original punctuation and formatting
- Character descriptions should be suitable for ElevenLabs voice generation
- If a character has little lore, make reasonable inferences based on their role and dialogue
- Create the `transcripts` directory if it doesn't exist
