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

### Step 4: Add per-line delivery tags

For **every** line — including the Player's — add an optional `delivery` field holding ElevenLabs v4 audio tags that direct how the line is spoken. Unlike descriptions (which skip the Player), delivery applies to Player lines too, since the Player is voiced. The pipeline wraps the value in brackets and prepends it to the line at synthesis, so store the cues **without brackets**.

1. **Base the delivery** on the line's content, and for NPCs the speaker's personality (from their description). For Player lines, base it on the line's content and the conversational moment.
2. **Use comma-separated cues**, at most two, e.g. `"annoyed"`, `"nervous, urgent"`, `"quietly, resigned"`, `"dry amusement"`, `"shouting"`, `"whispering"`.
3. **Be tasteful** — most lines need no delivery tag. Omit the field entirely for flat or purely functional lines. Do not tag every line.
4. **Never put words to be spoken in `delivery`** — it is direction only; the spoken words stay in `line`.

### Step 5: Update the JSON file

Add the `description` field to each character, and the `delivery` field to lines that warrant it.

Final JSON structure:
```json
{
  "branch": "quest/quest-name",
  "quest_name": "Quest Name",
  "lines": [
    { "character": "NPC Name", "line": "You'll never take me alive!", "delivery": "shouting, defiant" },
    { "character": "Player", "line": "Wait... you're the thief?", "delivery": "surprised" }
  ],
  "characters": [
    {
      "name": "NPC Name",
      "description": "Middle-aged male, British accent, gruff and impatient tone, a hardworking cook who takes pride in his work but is easily stressed."
    }
  ]
}
```

### Step 6: Validation

1. Count the number of `lines` in the generated JSON
2. Compare with the wiki page speaker count to verify extraction accuracy
3. Ensure all characters have descriptions
4. Ensure any `delivery` values are bare cues (no brackets) and contain no spoken words

## Important Notes

- Dialogue lines should preserve the original punctuation and formatting
- Character descriptions should be suitable for ElevenLabs voice generation
- If a character has little lore, make reasonable inferences based on their role and dialogue
- `delivery` is optional per line and affects only the generated audio, not the line's filename
