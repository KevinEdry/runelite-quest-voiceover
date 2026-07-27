# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Voice generation pipeline for the RuneLite Quest Voiceover plugin. This branch contains a self-hosted [Windmill](https://www.windmill.dev/) flow (in `windmill/`) that generates voice lines via the ElevenLabs API, uploads audio to GitHub, and populates the database used by the plugin.

## Build Commands

```bash
bun install                       # Install dependencies (for the scripts/ helpers)
bun run quest-order               # Run optimal quest order script
bun run typecheck                 # Type-check the scripts/ helpers
```

The pipeline itself lives in `windmill/` and runs as self-hosted Windmill flows. See
`windmill/README.md` for setup and running. Test a flow via the Windmill UI with
`dryRun: true` and a small `limit`.

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

### Reusable toolsets (`windmill/f/tools/`)
Domain modules imported by any flow's step scripts (Windmill resolves relative cross-folder
imports, e.g. `import { createGitHubClient } from "../tools/git"`). Shared-logic modules with
no `main`:
- **`voice.bun.ts`** - ElevenLabs client: setup/create voices, generate speech
- **`git.bun.ts`** - GitHub client: get/commit files, branches, audio upload
- **`database.bun.ts`** - SQLite-on-a-branch: download, query, insert, upload the plugin DB
- **`text.bun.ts`** - Dialogue hashing + template-token cleanup
- **`retry.bun.ts`** - Shared retry (429 / 5xx / 409) for the API clients
- **`types.bun.ts`** - Shared domain types

**File extension:** `.bun.ts` marks the Bun runtime (Windmill convention); required for
`bun:sqlite` / `Buffer`. `.bun.ts` modules only resolve under Windmill's bundler, not plain
`bun`/`tsc`.

### Flows (`windmill/f/`)
- **`quest_voiceover/`** - Main flow: setup_voices → expand_targets → generate_loop → write_database. `lib.bun.ts` is a barrel re-exporting the toolsets for this flow's steps.
- **`regenerate_female_voices/`** - Regenerates all Player Female lines (and fills the ones missing from the old professional-clone voice) onto a feature branch off `sounds`, one commit per line; adds the missing DB rows to a feature branch off `database`.

Secrets are Windmill variables (`f/quest_voiceover/*`); see `windmill/README.md`.

### Other Files
- **`pronunciation_dictionary.pls`** - ElevenLabs pronunciation dictionary
- **`transcripts/`** - Quest transcript JSON files extracted from OSRS Wiki
- **`scripts/`** - Helper scripts (extract-transcript, optimal-quest-order, smoke-test)

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
