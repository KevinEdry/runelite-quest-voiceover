# Extract Quest Transcript

Extract dialogue and character data from an OSRS Wiki quest transcript page.

**Input:** $ARGUMENTS (OSRS Wiki quest transcript URL, e.g., https://oldschool.runescape.wiki/w/Transcript:Cook%27s_Assistant)

## Instructions

### Step 1: Run the extraction script

Run the Python script to extract dialogue lines:

```bash
pixi run python3 scripts/extract_transcript.py "$ARGUMENTS"
```

This will:
- Fetch the wiki page and parse the HTML
- Extract the quest name from the page title
- Extract all dialogue lines with speaker names (skipping menu options, stage directions, etc.)
- Resolve dynamic text (remove `[Player name]`, `[1-19]`, etc.)
- Deduplicate identical lines
- Generate the branch name (`quest/quest-name-slug`)
- Save JSON to `transcripts/{quest-name-slug}.json`

### Step 2: Read the generated JSON

Read the generated transcript file to get the list of characters that need descriptions.

### Step 3: Add character descriptions

For each character in the `characters` array (excluding "Player"):

1. **Search the OSRS Wiki** for information about the character
2. **Create an ElevenLabs voice description** with this format: age, gender, accent, tone, personality
3. **Keep descriptions to 1-2 sentences**, max 500 characters
4. **Base the description** on the character's lore, appearance, and role in the game
5. **Never use the word "child"** in descriptions (ElevenLabs blocks it) - use alternatives like "young", "youthful", or specific ages instead

Example description:
```
"Middle-aged male, British accent, gruff and impatient tone, a hardworking cook who takes pride in his work but is easily stressed."
```

### Step 4: Update the JSON file

Add the `description` field to each character in the JSON file.

Final JSON structure:
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

### Step 5: Validation

1. Count the number of `lines` in the generated JSON
2. Compare with the wiki page speaker count to verify extraction accuracy
3. Ensure all characters have descriptions

## Important Notes

- Dialogue lines should preserve the original punctuation and formatting
- Character descriptions should be suitable for ElevenLabs voice generation
- If a character has little lore, make reasonable inferences based on their role and dialogue
