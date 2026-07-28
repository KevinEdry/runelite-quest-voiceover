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
    │   ├── list_quests.bun.ts  #   dropdown options: transcripts + generated indicator, ordered
    │   ├── load_transcript.bun.ts  # load the picked quest's lines + characters
    │   ├── plan_generation.bun.ts · approve_generation.bun.ts  # preview + human approval
    │   ├── setup_voices.bun.ts · expand_targets.bun.ts · generate_line.bun.ts · write_database.bun.ts
    │   └── quest_voiceover.flow/flow.yaml
    ├── regenerate_female_voices/   # Regenerate all Player Female lines onto a feature branch
    │   ├── prepare_female_regen.bun.ts   # list Player Female targets + create feature branch
    │   ├── generate_female_line.bun.ts   # regenerate + commit one line (parallel loop)
    │   ├── write_female_database.bun.ts  # add missing rows to a DB feature branch
    │   └── regenerate_female_voices.flow/flow.yaml
    ├── cleanup_voices/             # Delete ElevenLabs voices unused by future quests
    │   ├── analyze_unused_voices.bun.ts  # compute the deletable "generated" voices
    │   ├── approve_deletion.bun.ts       # suspend for human approval before deleting
    │   ├── delete_voice.bun.ts           # delete one voice (parallel loop)
    │   └── cleanup_voices.flow/flow.yaml
    └── clone_voices/               # Rebuild deleted voices via IVC from existing audio
        ├── plan_voice_clones.bun.ts      # which characters need cloning + candidate clips
        ├── approve_clones.bun.ts         # suspend for human approval before cloning
        ├── clone_character.bun.ts        # collect ~2 min of audio, create the IVC voice
        └── clone_voices.flow/flow.yaml
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

> `backfill-female-voices` is now covered by the `regenerate_female_voices` flow, and
> `cleanup-voices` by the `cleanup_voices` flow (both below).

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

Run the flow and just **pick a quest from the dropdown** — the `load_transcript` step then loads
that quest's lines and characters from the transcript, so you don't paste anything.

The dropdown is backed by `f/quest_voiceover/list_quests`, which lists every `transcripts/*.json`
with an indicator and orders **not-generated first**:

- `[ ] Cook's Assistant` — not yet voiced (no rows in the database)
- `[x] A Kingdom Divided` — already voiced

**Wiring the dropdown (one-time, in the UI):** open the flow, select the `quest` input, set its
type to **Dynamic Select**, and point it at `f/quest_voiceover/list_quests` (its `[{value, label}]`
return is the option list). The `value` is the transcript file path the flow loads. Without this
binding `quest` is still a plain text field — you'd type the path yourself, e.g.
`transcripts/cooks-assistant.json`.

The flow then **pauses for approval**: the approval page shows the quest, the character→voice
breakdown, which voices will be newly created, and the estimated ElevenLabs characters + cost
(at `costPer1kCharacters`, default `0.10`) against your remaining monthly quota. Approve to
proceed, reject to cancel. Because of this suspend step, don't schedule this flow unattended —
it would wait at approval until the timeout.

Set `dryRun: true` to synthesise audio without committing to GitHub or writing the database.

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

## Cleaning up voices

ElevenLabs has a limited voice count, so the `cleanup_voices` flow removes voices that
future quest generations won't use. A character's voice is **kept** if it appears in a quest
that hasn't been voiced yet (a `transcripts/*.json` whose `quest_name` isn't in the database);
voices used only by already-completed quests are removed.

Safety rails:
- Only **`generated`** voices (the ones this pipeline creates) are ever deleted — `premade`,
  `professional`, and `cloned` voices are always preserved.
- The configured `player_male_voice_id` / `player_female_voice_id` are never deleted.
- **Human approval before deleting** — after `analyze_unused_voices`, the flow suspends on an
  approval step showing the list. Approve to continue into the deletions, reject to cancel the
  flow (nothing is deleted). Configured via the module's `suspend` block (`required_events: 1`,
  24h `timeout`).

It reads the transcript list from the `transcripts/` directory on the `transcriptsBranch`
(default `automations`), so the flow knows the full quest roadmap.

## Rebuilding deleted voices (continuity)

ElevenLabs voice **design** (text-to-voice from a description) is **non-deterministic** — the same
description yields a different voice every time — so a deleted `generated` voice can't be recreated
from its description. But the character's **audio still exists** on the `sounds` branch, so the
`clone_voices` flow rebuilds the voice via **Instant Voice Cloning (IVC)** from those clips.

Why IVC specifically: PVC (professional cloning) isn't optimized for `eleven_v3` yet (this is what
broke the old professional Player Female), whereas **IVC works with v3**. For each character the flow
collects **~2 minutes** of their audio — IVC's sweet spot; more than ~3 min can *degrade* the clone,
and file count doesn't matter, only total duration — and creates a voice named after them.

IVC voices are category **`cloned`**, which `cleanup_voices` never deletes, so rebuilt voices stay put.
Run it before generating a quest whose recurring characters lost their voices (it defaults to Blood
Moon Rises' deleted recurring NPCs). Characters that already have a voice or lack audio are skipped,
and it pauses for approval before creating anything.

## Local development

```bash
wmill init                       # generates tsconfig.wmill.json so IDEs resolve ./lib imports
# edit *.bun.ts
wmill generate-metadata          # refresh .script.yaml schemas + lockfiles after signature changes
wmill sync push
```
