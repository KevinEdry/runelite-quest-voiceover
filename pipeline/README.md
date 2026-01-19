# Quest Voiceover Pipeline

Temporal.io-based pipeline for generating OSRS quest voiceovers using ElevenLabs.

## Prerequisites

- [Bun](https://bun.sh/) runtime
- Docker and Docker Compose (for Temporal server)
- ElevenLabs API key
- GitHub personal access token with repo scope

## Setup

1. Install dependencies:

```bash
cd pipeline
bun install
```

2. Copy environment variables:

```bash
cp .env.example .env
```

3. Edit `.env` with your credentials:

```env
ELEVENLABS_API_KEY=your_api_key
GITHUB_TOKEN=your_github_token
GITHUB_OWNER=KevinEdry
GITHUB_REPO=runelite-quest-voiceover
PLAYER_MALE_VOICE_ID=your_male_player_voice_id
PLAYER_FEMALE_VOICE_ID=your_female_player_voice_id
TEMPORAL_ADDRESS=localhost:7233

# Testing mode - skips GitHub uploads (audio files and database)
DRY_RUN=false
```

## Running

### 1. Start Temporal Server

```bash
docker compose up -d
```

Wait for Temporal to be healthy (check with `docker compose ps`).

### 2. Start Workers

In separate terminals (or use a process manager):

```bash
# Terminal 1: Voice setup worker
bun run worker:voice

# Terminal 2: TTS worker
bun run worker:tts

# Terminal 3: Upload worker
bun run worker:upload
```

Or run all workers together:

```bash
bun run workers
```

### 3. Process Quests

List available quests:

```bash
bun run cli list
```

Process a specific quest:

```bash
bun run cli process --quest heroes-quest
```

Or use interactive mode:

```bash
bun run cli interactive
```

## Testing Mode

To test without pushing to GitHub, set `DRY_RUN=true` in your `.env`:

```env
DRY_RUN=true
```

In dry-run mode:
- ✅ Voice generation still happens (consumes ElevenLabs credits)
- ✅ Database is saved locally at `pipeline/output_db/quest_voiceover.db`
- ❌ No audio files uploaded to GitHub `sounds` branch
- ❌ No database uploaded to GitHub `database` branch

Perfect for testing the pipeline before committing to GitHub.

## Monitoring

Open the Temporal Web UI at http://localhost:8080 to:

- View workflow execution progress
- Inspect activity success/failure
- Check retry attempts
- Debug with input/output data

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Temporal Server (Docker)                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Workflow  │  │voice-setup │  │    tts     │  │   upload   │  Web   │
│  │  History   │  │   queue    │  │   queue    │  │   queue    │  UI    │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘ :8080  │
└─────────────────────────────────────────────────────────────────────────┘
         │                │                │                │
         ▼                ▼                ▼                ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Client    │   │Voice Worker │   │ TTS Worker  │   │Upload Worker│
│    (CLI)    │   │ rate: 10/m  │   │ rate: 60/m  │   │ rate: 30/m  │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
```

### Task Queues

- **voice-setup**: Voice matching and creation (10 req/min)
- **tts**: Text-to-speech generation (60 req/min)
- **upload**: GitHub uploads and database updates (30 req/min)

## File Structure

```
pipeline/
├── docker-compose.yml          # Temporal server + PostgreSQL
├── package.json
├── tsconfig.json
├── pronunciation_dictionary.pls
├── src/
│   ├── cli.ts                  # CLI entry point
│   ├── client.ts               # Temporal client setup
│   ├── workflows/
│   │   └── quest-voiceover.ts  # Main workflow definition
│   ├── activities/
│   │   ├── voice-setup.ts      # Voice matching/creation
│   │   ├── tts.ts              # ElevenLabs TTS
│   │   ├── github.ts           # GitHub file operations
│   │   └── database.ts         # SQLite operations
│   ├── workers/
│   │   ├── voice-worker.ts     # Worker for voice-setup queue
│   │   ├── tts-worker.ts       # Worker for tts queue
│   │   └── upload-worker.ts    # Worker for upload queue
│   ├── services/
│   │   ├── elevenlabs.ts       # ElevenLabs API client
│   │   ├── github.ts           # GitHub API client
│   │   └── rate-limiter.ts     # Token bucket rate limiter
│   └── types/
│       └── index.ts            # TypeScript types
└── dynamicconfig/
    └── development-sql.yaml    # Temporal config
```

## Troubleshooting

### Temporal won't start

Check PostgreSQL is healthy:

```bash
docker compose logs postgresql
```

### Workers can't connect

Ensure Temporal is running and healthy:

```bash
docker compose ps
```

### Rate limit errors

The pipeline includes built-in rate limiting, but if you see 429 errors, reduce worker concurrency in the worker files.
