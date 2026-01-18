#!/usr/bin/env bun
import * as fs from "fs";
import * as path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const TRANSCRIPTS_DIR = path.resolve(__dirname, "../../transcripts");

interface QuestTranscript {
  quest_name: string;
  characters: { name: string }[];
}

function loadTranscripts(): QuestTranscript[] {
  return fs
    .readdirSync(TRANSCRIPTS_DIR)
    .filter((file) => file.endsWith(".json"))
    .map((file) => {
      const content = fs.readFileSync(path.join(TRANSCRIPTS_DIR, file), "utf-8");
      return JSON.parse(content) as QuestTranscript;
    });
}

function getCharacterSet(quest: QuestTranscript): Set<string> {
  return new Set(quest.characters.map((c) => c.name));
}

function computeOptimalOrder(
  transcripts: readonly QuestTranscript[],
  completedQuests: readonly string[]
): { quest: string; newVoices: number; reusedVoices: number }[] {
  const completedSet = new Set(completedQuests);
  const remaining = transcripts.filter((t) => !completedSet.has(t.quest_name));

  const knownVoices = new Set<string>();
  for (const quest of transcripts) {
    if (completedSet.has(quest.quest_name)) {
      for (const char of quest.characters) {
        knownVoices.add(char.name);
      }
    }
  }

  const result: { quest: string; newVoices: number; reusedVoices: number }[] = [];
  const processed = new Set<string>();

  while (processed.size < remaining.length) {
    let bestQuest: QuestTranscript | null = null;
    let bestScore = -1;
    let bestNewVoices = 0;
    let bestReusedVoices = 0;

    for (const quest of remaining) {
      if (processed.has(quest.quest_name)) continue;

      const chars = getCharacterSet(quest);
      let reused = 0;
      let newCount = 0;

      for (const char of chars) {
        if (knownVoices.has(char)) {
          reused++;
        } else {
          newCount++;
        }
      }

      const score = reused - newCount;

      if (score > bestScore || bestQuest === null) {
        bestScore = score;
        bestQuest = quest;
        bestNewVoices = newCount;
        bestReusedVoices = reused;
      }
    }

    if (bestQuest) {
      processed.add(bestQuest.quest_name);
      result.push({
        quest: bestQuest.quest_name,
        newVoices: bestNewVoices,
        reusedVoices: bestReusedVoices,
      });

      for (const char of bestQuest.characters) {
        knownVoices.add(char.name);
      }
    }
  }

  return result;
}

async function main() {
  const transcripts = loadTranscripts();
  console.log(`Loaded ${transcripts.length} transcripts\n`);

  const completedQuests: string[] = [];

  const dbPath = path.resolve(__dirname, "../output_db/quest_voiceover.db");
  if (fs.existsSync(dbPath)) {
    const { Database } = await import("bun:sqlite");
    const db = new Database(dbPath, { readonly: true });
    const rows = db.query("SELECT DISTINCT quest FROM dialogs").all() as { quest: string }[];
    completedQuests.push(...rows.map((r) => r.quest));
    db.close();
    console.log(`Found ${completedQuests.length} completed quests in database\n`);
  }

  const order = computeOptimalOrder(transcripts, completedQuests);

  console.log("Optimal quest order (maximizes voice reuse):\n");
  console.log("Quest".padEnd(50) + "New".padStart(6) + "Reused".padStart(8));
  console.log("-".repeat(64));

  let totalNew = 0;
  let totalReused = 0;

  for (const entry of order) {
    console.log(
      entry.quest.padEnd(50) +
      entry.newVoices.toString().padStart(6) +
      entry.reusedVoices.toString().padStart(8)
    );
    totalNew += entry.newVoices;
    totalReused += entry.reusedVoices;
  }

  console.log("-".repeat(64));
  console.log("Total".padEnd(50) + totalNew.toString().padStart(6) + totalReused.toString().padStart(8));
}

main().catch(console.error);
