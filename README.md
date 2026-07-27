# Quest Voiceover Pipeline

Voice-generation pipeline for the RuneLite Quest Voiceover plugin. It generates
voice lines via the ElevenLabs API, uploads the audio to the `sounds` branch on
GitHub, and writes the plugin's SQLite database on the `database` branch.

The pipeline runs as a self-hosted [Windmill](https://www.windmill.dev/) flow. See
[`windmill/README.md`](./windmill/README.md) for setup, secrets, and how to run a full
quest.

## Layout

```
transcripts/          # Quest transcript JSON extracted from the OSRS Wiki
pronunciation_dictionary.pls
scripts/
  extract-transcript.ts    # Scrape a quest transcript from the wiki
  optimal-quest-order.ts   # Compute an optimal quest processing order
windmill/             # The Windmill workspace: the pipeline + reusable toolsets — see its README
```

## Setup

```bash
bun install               # only for the helper scripts in scripts/
```

The pipeline itself lives in `windmill/` and is configured with Windmill **variables** (not
`.env`). See [`windmill/README.md`](./windmill/README.md) for secrets, running a quest, and
regenerating voice lines.

## Testing the generation flow

Testing happens inside Windmill: open a flow, set `dryRun: true` and a small `limit`, and
**Run** it from the UI. Dry-run synthesises audio (so you can confirm generation works)
without committing to GitHub or writing the database.

> Even in dry-run, ElevenLabs text-to-speech is called for real and consumes credits — keep
> the sample small.

## Audio file naming

Audio files use the MD5 hash of `{character}|{dialog_text}` as the filename (e.g.
`a1b2c3d4.mp3`), keeping file references consistent between the database and the `sounds`
branch.

## Branch structure

- **`main`** — plugin source (Java) and build configs
- **`automations`** — this pipeline (TypeScript / Windmill)
- **`sounds`** — MP3 audio files only, served via raw GitHub URLs at runtime
- **`database`** — the SQLite database file, downloaded by the plugin at startup
