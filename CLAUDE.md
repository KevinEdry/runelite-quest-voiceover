# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuneLite plugin that adds AI-generated voice acting to Old School RuneScape quest dialogues. The project has two main components:

1. **RuneLite Plugin (Java)** - Listens for in-game dialog events, looks up audio files from a SQLite database, and streams MP3 audio from GitHub
2. **Voice Generation CLI (Python)** - Scrapes OSRS Wiki for quest transcripts, generates voice lines via ElevenLabs API, and populates the database

## Build Commands

### Java Plugin
```bash
./gradlew build          # Build the plugin
./gradlew test           # Run tests
./gradlew clean build    # Clean rebuild
```

### Python CLI
```bash
pixi install             # Install dependencies
pixi run cli             # Run interactive CLI to generate voiceovers
pixi shell               # Activate virtual environment (optional)
```

## Architecture

### Java Plugin (`src/main/java/com/quest/voiceover/`)
- **QuestVoiceoverPlugin.java** - Main plugin entry point. Subscribes to RuneLite events (`ChatMessage`, `MenuOptionClicked`, `WidgetLoaded/Closed`) to detect quest dialogs and trigger audio playback
- **SoundEngine.java** - Handles MP3 streaming from GitHub's `sounds` branch using the Jaco MP3 player library
- **DialogEngine.java** - Manages dialog UI widgets (mute button, quest name display)
- **DatabaseManager.java** - SQLite connection management for the dialog lookup database
- **DatabaseVersionManager.java** - Downloads and version-controls the database from GitHub's `database` branch
- **MessageUtils.java** - Parses chat messages to extract character name and dialog text
- **HashUtil.java** - MD5 hashing for audio file naming

### Python CLI (`voiceover_cli/`)
- **wiki_utils.py** - Scrapes OSRS Wiki for quest transcripts and character lists
- **elevenlabs.py** - ElevenLabs SDK wrapper for voice generation
- **database.py** - SQLite FTS4 virtual table management for dialog lookups
- **utils.py** - Utility functions (MD5 hashing, text processing)

### Git Branch Structure (Unconventional)

This repo uses separate orphan branches for different content types:

- **`main`** - Plugin source code (Java), CLI tools (Python), build configs
- **`sounds`** - MP3 audio files only (~1300+ files). No code. Files served via raw GitHub URLs at runtime
- **`database`** - SQLite database file (`quest_voiceover.db`) only. No code. Downloaded by plugin at startup

The `sounds` and `database` branches share no commit history with `main`. They function as asset storage/CDN.

## Audio File Naming

Audio files use MD5 hash of `{character}|{dialog_text}` as filename (e.g., `a1b2c3d4.mp3`). This ensures consistent file references between the database and the sounds branch.

## Database Schema

```sql
CREATE VIRTUAL TABLE dialogs USING fts4(
    quest TEXT NOT NULL UNINDEXED,
    character TEXT NOT NULL,
    text TEXT NOT NULL,
    uri TEXT NOT NULL UNINDEXED
)
```

FTS4 enables fast full-text search matching of in-game dialog text to audio files.

## Contributing Voice Lines

1. Create ElevenLabs voices for quest characters
2. Run `python cli-main.py` and follow prompts
3. Generated MP3s go to `output_voiceover/`, database to `output_db/`
4. Switch to `sounds` branch, add MP3 files, commit with message format: `feat: Add sound for quest {Quest} character: {Character} line: {Line}`
5. Switch to `database` branch, update `quest_voiceover.db`, commit
