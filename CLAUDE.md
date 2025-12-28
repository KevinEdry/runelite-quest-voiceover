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

## Code Style

### General Principles
- Main plugin file should only contain event handlers - delegate all logic to specialized managers
- Good separation of concerns - each class has a single responsibility
- Readable code with descriptive method and variable names - avoid abbreviations (e.g., use `source` not `s1`, `previousRow` not `prevRow`)
- Descriptive conditions - extract complex conditionals into well-named methods
- No "what" or "how" comments - only "why" comments when the reasoning isn't obvious
- Use early returns to reduce nesting

### File Organization
- **Localized code**: Feature-specific logic goes in `features/` folder (flat structure, no subdirectories)
- **Shared code**: Reusable infrastructure goes in `modules/` folder
- **Utilities**: Generic helpers go in `utility/` folder (outside modules)

### Git Commits
- Never use AI attestation in commits (no robot emoji, no "Generated with Claude", no Co-Authored-By AI lines)

## Architecture

### Java Plugin (`src/main/java/com/quest/voiceover/`)

```
com/quest/voiceover/
├── QuestVoiceoverPlugin.java      # Event handlers only
├── QuestVoiceoverConfig.java      # Plugin configuration
│
├── modules/                        # Shared infrastructure
│   ├── audio/
│   │   ├── AudioManager.java         # MP3 playback
│   │   ├── AudioDuckingManager.java  # Game audio ducking during voiceover
│   │   └── AudioChannelsManager.java # Game volume control (percentage/absolute conversion)
│   ├── database/
│   │   ├── DatabaseManager.java   # SQLite connection & queries
│   │   └── DatabaseVersionManager.java  # Database download/versioning
│   └── dialog/
│       └── DialogManager.java     # Dialog widget manipulation
│
├── features/                       # Business logic (flat structure)
│   ├── VoiceoverHandler.java      # Plays voiceovers on dialog
│   └── QuestListIndicatorHandler.java  # Quest list [Voiced] indicators
│
└── utility/                        # Shared utilities (stateless pure functions)
    ├── HashUtility.java           # MD5/SHA hashing
    └── MessageUtility.java        # Dialog message parsing and cleaning
```

### Python CLI (`voiceover_cli/`)
- **wiki_utils.py** - Scrapes OSRS Wiki for quest transcripts and character lists
- **elevenlabs.py** - ElevenLabs SDK wrapper for voice generation
- **database.py** - SQLite table management for dialog lookups
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
CREATE TABLE dialogs (
    quest TEXT NOT NULL,
    character TEXT NOT NULL,
    text TEXT NOT NULL,
    uri TEXT NOT NULL
);
CREATE INDEX idx_dialogs_character ON dialogs(character);
CREATE INDEX idx_dialogs_character_text ON dialogs(character, text);
```

Dialog lookups use exact match and Levenshtein similarity fallback for fuzzy matching.

## Contributing Voice Lines

1. Create ElevenLabs voices for quest characters
2. Run `python cli-main.py` and follow prompts
3. Generated MP3s go to `output_voiceover/`, database to `output_db/`
4. Switch to `sounds` branch, add MP3 files, commit with message format: `feat: Add sound for quest {Quest} character: {Character} line: {Line}`
5. Switch to `database` branch, update `quest_voiceover.db`, commit
