# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Voice generation CLI tools for the RuneLite Quest Voiceover plugin. This branch contains Python tools that scrape OSRS Wiki for quest transcripts, generate voice lines via ElevenLabs API, and populate the database used by the plugin.

## Build Commands

```bash
pixi install             # Install dependencies
pixi run cli             # Run interactive CLI to generate voiceovers
pixi shell               # Activate virtual environment (optional)
```

## Code Style

### General Principles
- Readable code with descriptive method and variable names - avoid abbreviations
- No "what" or "how" comments - only "why" comments when the reasoning isn't obvious
- Use early returns to reduce nesting

### Git Commits
- Never use AI attestation in commits (no robot emoji, no "Generated with Claude", no Co-Authored-By AI lines)

### TypeScript Pipeline (`pipeline/`)
- File naming convention: suffix files with their domain (e.g., `database.provider.ts`, `github.client.ts`)
- Folder structure:
  - `clients/` - API client abstractions (e.g., `github.client.ts`, `elevenlabs.client.ts`)
  - `providers/` - Data providers (e.g., `database.provider.ts`)
  - `utilities/` - Utility functions grouped by logical domain (e.g., `text.util.ts`, `hash.util.ts`)
- No mutability in code - prefer immutable data structures and pure functions
- Imperative shell, functional core - no classes, use functions
- Logic should be related - group functions by their logical domain, not by which service uses them
- Avoid abbreviations in variable and function names

## Architecture

### Python CLI (`voiceover_cli/`)
- **wiki_utils.py** - Scrapes OSRS Wiki for quest transcripts and character lists
- **elevenlabs.py** - ElevenLabs SDK wrapper for voice generation
- **database.py** - SQLite table management for dialog lookups
- **utils.py** - Utility functions (MD5 hashing, text processing)

### Other Files
- **cli-main.py** - CLI entry point
- **pronunciation_dictionary.pls** - ElevenLabs pronunciation dictionary
- **transcripts/** - Quest transcript JSON files extracted from OSRS Wiki
- **scripts/** - Helper scripts (extract_transcript.py)

### Git Branch Structure

This repo uses separate orphan branches for different content types:

- **`main`** - Plugin source code (Java) and build configs
- **`automations`** - Voice generation CLI tools (Python) - *this branch*
- **`sounds`** - MP3 audio files only (~1300+ files). No code. Files served via raw GitHub URLs at runtime
- **`database`** - SQLite database file (`quest_voiceover.db`) only. No code. Downloaded by plugin at startup

## Audio File Naming

Audio files use MD5 hash of `{character}|{dialog_text}` as filename (e.g., `a1b2c3d4.mp3`). This ensures consistent file references between the database and the sounds branch.

## Database Schema

```sql
CREATE TABLE dialogs (
    quest TEXT NOT NULL,
    character TEXT NOT NULL,
    text TEXT NOT NULL,
    uri TEXT NOT NULL
);
CREATE INDEX idx_dialogs_character ON dialogs(character);
CREATE INDEX idx_dialogs_character_text ON dialogs(character, text);
```

## Contributing Voice Lines

1. Create ElevenLabs voices for quest characters
2. Run `python cli-main.py` and follow prompts
3. Generated MP3s go to `output_voiceover/`, database to `output_db/`
4. Switch to `sounds` branch, add MP3 files, commit with message format: `feat: Add sound for quest {Quest} character: {Character} line: {Line}`
5. Switch to `database` branch, update `quest_voiceover.db`, commit
