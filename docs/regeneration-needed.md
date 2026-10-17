# Quests to regenerate (missing NPC voice lines)

These quests were generated while ElevenLabs **voice creation was failing** (voice-slot cap / low quota reached mid-batch). Their new-voice NPC characters were silently skipped, so only the player lines (and NPCs whose voice already existed) landed. The runs still reported success and were merged, so the database marks them done with incomplete audio.

**19 quests affected, ~2,812 NPC lines missing.**

## How to fix

Re-run each quest through the `quest_voiceover` flow. A re-run only generates the **missing** clips — every clip already on the `sounds` branch (all player lines, any NPC lines that did land) is skipped and not billed, and `write_database` overwrites the incomplete shard with the complete one.

The one requirement: the re-run must **successfully create the NPC voices this time**, so run with enough **free voice slots** and **character quota** — ideally **one or two quests at a time**, not a large parallel batch (parallel batches race on voice creation and can re-trigger the same cap failure).

## Affected quests

| Quest | NPC lines missing | % of NPC lines | Transcript |
|-------|------------------:|---------------:|------------|
| Between a Rock... | 232 | 100% | `transcripts/between-a-rock.json` |
| Tower of Life | 222 | 100% | `transcripts/tower-of-life.json` |
| Troubled Tortugans | 149 | 100% | `transcripts/troubled-tortugans.json` |
| The Ribbiting Tale of a Lily Pad Labour Dispute | 145 | 100% | `transcripts/the-ribbiting-tale-of-a-lily-pad-labour-dispute.json` |
| Scrambled! | 143 | 100% | `transcripts/scrambled.json` |
| Fishing Contest | 120 | 100% | `transcripts/fishing-contest.json` |
| Meat and Greet | 106 | 100% | `transcripts/meat-and-greet.json` |
| Fairytale II - Cure a Queen | 113 | 99% | `transcripts/fairytale-ii-cure-a-queen.json` |
| Perilous Moons | 311 | 97% | `transcripts/perilous-moons.json` |
| Making History | 127 | 96% | `transcripts/making-history.json` |
| Fairytale I - Growing Pains | 131 | 90% | `transcripts/fairytale-i-growing-pains.json` |
| Swan Song | 166 | 78% | `transcripts/swan-song.json` |
| Merlin's Crystal | 74 | 78% | `transcripts/merlins-crystal.json` |
| Holy Grail | 83 | 70% | `transcripts/holy-grail.json` |
| Bone Voyage | 101 | 68% | `transcripts/bone-voyage.json` |
| The Great Brain Robbery | 167 | 67% | `transcripts/the-great-brain-robbery.json` |
| Underground Pass | 112 | 65% | `transcripts/underground-pass.json` |
| The Slug Menace | 188 | 60% | `transcripts/the-slug-menace.json` |
| Biohazard | 122 | 54% | `transcripts/biohazard.json` |

## Root-cause fix (pipeline)

`setup_voices` currently catches a failed voice creation and continues (resilient-skip, meant for a single safety-blocked voice). When creation fails en masse it should **fail the run and not open PRs**, so an incomplete quest is never silently merged. Until that’s in place, verify a run created its expected voices before merging.
