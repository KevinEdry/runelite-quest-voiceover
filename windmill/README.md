# Quest Voiceover — Windmill pipeline

Self-hosted [Windmill](https://www.windmill.dev/) replacement for the Restate voice-generation
service. This directory is a Windmill workspace synced to git via the `wmill` CLI.

## What's here

```
windmill/
├── docker-compose.yml          # Self-hosted Windmill (server + worker + Postgres)
├── wmill.yaml                  # wmill CLI git-sync config
└── f/
    ├── tools/                  # Reusable domain toolsets (shared logic, no main)
    │   ├── voice.bun.ts        #   ElevenLabs: setup/create voices, generate speech
    │   ├── git.bun.ts          #   GitHub: get/commit files, branches, audio upload
    │   ├── database.bun.ts     #   SQLite-on-a-branch: download, query, insert, upload
    │   ├── text.bun.ts         #   dialogue hashing + template-token cleanup
    │   ├── retry.bun.ts        #   shared retry (429 / 5xx / 409)
    │   └── types.bun.ts        #   shared domain types
    ├── quest_voiceover/        # Main flow (generate a quest's voice lines)
    │   ├── lib.bun.ts          #   barrel re-exporting ../tools/* for this flow's steps
    │   ├── setup_voices.bun.ts · expand_targets.bun.ts · generate_line.bun.ts · write_database.bun.ts
    │   └── quest_voiceover.flow/flow.yaml
    └── regenerate_female_voices/   # Regenerate all Player Female lines onto a feature branch
        ├── prepare_female_regen.bun.ts   # list Player Female targets + create feature branch
        ├── generate_female_line.bun.ts   # regenerate + commit one line (parallel loop)
        ├── write_female_database.bun.ts  # add missing rows to a DB feature branch
        └── regenerate_female_voices.flow/flow.yaml
```

## Toolsets

Flows are thin glue over the modules in `f/tools/`. A step script imports the toolset it needs
via a relative path — Windmill resolves relative imports across folders:

```ts
import { createGitHubClient } from "../tools/git";
import { createElevenLabsClient } from "../tools/voice";
```

`f/tools/*` modules have no `main`, so Windmill treats them as shared logic. `f/quest_voiceover/lib.bun.ts`
is a compatibility barrel (`export * from "../tools/*"`) so that flow's existing steps keep importing
`./lib`. New flows should import the specific toolset directly.

> **`.bun.ts`** marks the Bun runtime (needed for `bun:sqlite` / `Buffer`). These modules only
> resolve under Windmill's bundler — plain `bun`/`tsc` can't resolve `.bun.ts` cross-imports, so
> test flows inside Windmill (see below), not with a local script.

## How the flow maps to the old Restate workflow

| Restate (`quest-voiceover.workflow.ts`)          | Windmill                                            |
| ------------------------------------------------ | --------------------------------------------------- |
| `ctx.run("setup-voices", …)`                     | `setup_voices` step                                 |
| in-process `for (line of lines)` loop            | `generate_loop` — a **parallel** for-loop (≤3 at a time) |
| per-line `ctx.run` checkpoints (hash/exists/tts/upload) | one `generate_line` job per target               |
| scattered per-line `database.insertDialog`       | one `write_db` step at the end                      |
| in-memory token-bucket rate limiter              | flow `parallelism` + retry-on-429 inside `lib.bun.ts` |
| `PLAYER_*_VOICE_ID` / `GITHUB_TOKEN` env vars    | Windmill secret variables (see below)               |

**Why the database write moved to the end:** Windmill runs every loop iteration as a
separate stateless job, so the old "download the SQLite file, mutate it in-process, re-upload"
pattern can't be shared across steps. Audio is generated *and* uploaded inside the same job
(the MP3 buffer never crosses a step boundary), and the database is downloaded, updated with all
results, and re-uploaded exactly once in `write_db`.

> `backfill-female-voices` is now covered by the `regenerate_female_voices` flow (below).
> Not migrated yet: `cleanup-voices` — a small flow reusing `../tools/voice`.

## Setup

### 1. Start Windmill

```bash
cd windmill
docker compose up -d
```

Open http://localhost:8000, create the first (superadmin) user, and create a workspace —
e.g. id `quest_voiceover`.

### 2. Install the CLI and sync this folder

```bash
bun install -g windmill-cli      # or: npm i -g windmill-cli
cd windmill
wmill workspace add quest_voiceover quest_voiceover http://localhost:8000
wmill sync push                  # pushes f/** (scripts + flow) to the workspace
```

### 3. Create the variables

| Path                                          | Secret | Value                                        |
| --------------------------------------------- | ------ | -------------------------------------------- |
| `f/quest_voiceover/elevenlabs_api_key`        | yes    | ElevenLabs API key                           |
| `f/quest_voiceover/github_token`              | yes    | GitHub token with write access to the repo   |
| `f/quest_voiceover/player_male_voice_id`      | no     | default Player Male voice id (Chris)         |
| `f/quest_voiceover/player_female_voice_id`    | no     | default Player Female voice id (Jessica)     |

> **Secrets vs non-secrets, and `wmill sync push`.** `push` reconciles the workspace to the
> local files (`skipVariables: false`), so it **deletes any non-secret variable that isn't a
> committed file**. The two `player_*_voice_id` variables are therefore committed as
> `*.variable.yaml` files (voice IDs aren't secret) so push recreates rather than removes them.
> The two **secrets** are NOT committed — set them once in the UI (Variables → Add); they're
> protected from deletion by `skipSecrets: true`. If you rebuild the instance, only the two
> secrets need re-entering.

The two `player_*_voice_id` variables are only used when the matching flow input is left blank.

## Running a quest

The flow takes the same data as a `transcripts/*.json` file. Build the run arguments from one:

```bash
jq '{
  questName: .quest_name,
  lines: .lines,
  characters: .characters,
  githubOwner: "YOUR_GH_USER",
  githubRepo: "runelite-quest-voiceover",
  dryRun: false
}' ../transcripts/a-kingdom-divided.json > /tmp/args.json
```

Then either paste the fields into the flow's run form in the UI, or run headless:

```bash
wmill flow run f/quest_voiceover/quest_voiceover --data @/tmp/args.json
```

Set `dryRun: true` to synthesise audio without uploading to GitHub or writing the database.

### Scheduling

Add a schedule in the UI (Schedules → New) pointing at `f/quest_voiceover/quest_voiceover`
with a cron expression and a fixed argument payload, or `wmill schedule` via the CLI.

## Regenerating Player Female voices

The `regenerate_female_voices` flow re-synthesises the Player Female lines with the configured
premade voice (replacing audio from the old professional clone) and fills in the lines the clone
failed to produce under v3. It reads the Player Male rows from the database as the authoritative
set of player lines, generates a Player Female line for each, and **commits each line to a feature
branch off `sounds`** (one commit per line — no PR is opened). The previously-missing dialog rows
are written to a matching `…-db` feature branch off `database`. Merge both branches together when
you're happy with the result.

Run it from the Windmill UI (**Run** on the flow page). Key inputs:

| Input | Default | Meaning |
| --- | --- | --- |
| `forceRegenerate` | `true` | Regenerate every Player Female line. `false` = only the lines missing from the DB. |
| `limit` | _(blank)_ | Cap the number of lines. Use a small value for a first run. |
| `dryRun` | `false` | Synthesise without committing. **ElevenLabs is still billed.** |
| `resume` | `false` | Skip lines already regenerated on this feature branch (see below). |
| `updateDatabase` | `true` | Write the missing rows to the `…-db` feature branch. |
| `featureBranch` | _(auto)_ | `regen-female-voices-<date>`; reused if it already exists. |
| `playerFemaleVoiceId` | _(variable)_ | Falls back to `f/quest_voiceover/player_female_voice_id` (Jessica). |

> Regenerating **all** female lines is a large, billable job (one ElevenLabs call per line). Start
> with `dryRun: true` + `limit: 3` to confirm it works, then a real run with a small `limit`, then
> the full run.

### Resuming a broken run

Each line is its own commit and `skip_failures` keeps the flow going, so work done before a crash
survives on the feature branch. To continue without redoing it:

1. Re-run with **`featureBranch`** set to the branch the interrupted run created (e.g.
   `regen-female-voices-20260726-1530`) — it's reused, not recreated.
2. Set **`resume: true`**.

`resume` skips any line whose file on the feature branch already differs from the `sounds` baseline
(i.e. we wrote it in the earlier attempt) — a cheap sha comparison, no ElevenLabs cost for skips.
The final database step still records every line that has a file (completed or resumed-skip), so the
`…-db` branch ends up complete regardless of how many attempts it took.

## Local development

```bash
wmill init                       # generates tsconfig.wmill.json so IDEs resolve ./lib imports
# edit *.bun.ts
wmill generate-metadata          # refresh .script.yaml schemas + lockfiles after signature changes
wmill sync push
```
