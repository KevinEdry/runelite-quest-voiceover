# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Voice generation pipeline for the RuneLite Quest Voiceover plugin. This branch contains a TypeScript service built on [Restate](https://restate.dev/) that generates voice lines via ElevenLabs API, uploads audio to GitHub, and populates the database used by the plugin.

## Build Commands

```bash
bun install                       # Install dependencies
bun run service                   # Start the Restate service (src/service.ts)
bun run quest-order               # Run optimal quest order script
docker compose up                 # Start Restate server
```

## Code Style

### General Principles
- Readable code with descriptive method and variable names - avoid abbreviations
- No "what" or "how" comments - only "why" comments when the reasoning isn't obvious
- Use early returns to reduce nesting

### Git Commits
- Never use AI attestation in commits (no robot emoji, no "Generated with Claude", no Co-Authored-By AI lines)

### TypeScript
- File naming convention: suffix files with their domain (e.g., `database.provider.ts`, `github.client.ts`)
- Folder structure:
  - `clients/` - API client abstractions (e.g., `github.client.ts`, `elevenlabs.client.ts`)
  - `providers/` - Data providers (e.g., `database.provider.ts`)
  - `utilities/` - Utility functions grouped by logical domain (e.g., `text.util.ts`, `hash.util.ts`)
  - `workflows/` - Restate workflow definitions, each in its own folder
- Use `@/` path alias for cross-folder imports (e.g., `import { foo } from "@/clients/foo.client.js"`); use `./` only for sibling imports within the same folder
- No mutability in code - prefer immutable data structures and pure functions
- Imperative shell, functional core - no classes, use functions
- Logic should be related - group functions by their logical domain, not by which service uses them
- Avoid abbreviations in variable and function names

## Architecture

### Source Layout (`src/`)
- **`service.ts`** - Restate service entry point, registers all workflows
- **`clients/`** - API client abstractions (ElevenLabs, GitHub)
- **`providers/`** - Data providers (SQLite database)
- **`types/`** - Shared TypeScript types
- **`utilities/`** - Utility functions (text processing, hashing)
- **`workflows/`** - Restate workflow definitions:
  - `quest-voiceover/` - Main workflow: generates voice lines for a quest
  - `backfill-female-voices/` - Backfills Player Female voice lines from existing Player Male lines
  - `cleanup-voices/` - Removes unused ElevenLabs voices

### Other Files
- **`pronunciation_dictionary.pls`** - ElevenLabs pronunciation dictionary
- **`transcripts/`** - Quest transcript JSON files extracted from OSRS Wiki
- **`scripts/`** - Helper scripts (extract-transcript, optimal-quest-order)

### Git Branch Structure

This repo uses separate orphan branches for different content types:

- **`main`** - Plugin source code (Java) and build configs
- **`automations`** - Voice generation pipeline (TypeScript/Restate) - *this branch*
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
