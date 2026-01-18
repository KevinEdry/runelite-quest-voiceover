import * as restate from "@restatedev/restate-sdk";
import * as fs from "fs";
import * as path from "path";
import { fileURLToPath } from "url";
import type { CleanupVoicesInput, CleanupVoicesResult, QuestTranscript } from "../types/index.js";
import { getElevenLabsClient } from "../clients/elevenlabs.client.js";
import { getDatabaseProvider, resetDatabaseProvider } from "../providers/database.provider.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const TRANSCRIPTS_DIR = path.resolve(__dirname, "../../../transcripts");

function loadAllTranscripts(): QuestTranscript[] {
  if (!fs.existsSync(TRANSCRIPTS_DIR)) {
    return [];
  }

  return fs
    .readdirSync(TRANSCRIPTS_DIR)
    .filter((file) => file.endsWith(".json"))
    .map((file) => {
      const content = fs.readFileSync(path.join(TRANSCRIPTS_DIR, file), "utf-8");
      return JSON.parse(content) as QuestTranscript;
    });
}

export const cleanupVoicesWorkflow = restate.workflow({
  name: "cleanupVoices",
  handlers: {
    run: async (
      ctx: restate.WorkflowContext,
      input: CleanupVoicesInput
    ): Promise<CleanupVoicesResult> => {
      const dryRun = input.dryRun ?? false;

      resetDatabaseProvider();

      const elevenlabs = getElevenLabsClient();
      const database = getDatabaseProvider();

      const { neededCharacters, completedQuests, remainingQuests } = await ctx.run(
        "analyze-transcripts",
        async () => {
          const allTranscripts = loadAllTranscripts();
          const completed = await database.getCompletedQuests();

          const remaining = allTranscripts.filter(
            (t) => !completed.includes(t.quest_name)
          );

          const needed = new Set<string>();
          for (const transcript of remaining) {
            for (const character of transcript.characters) {
              needed.add(character.name);
            }
          }

          return {
            neededCharacters: [...needed].sort(),
            completedQuests: [...completed],
            remainingQuests: remaining.map((t) => t.quest_name),
          };
        }
      );

      console.log(`Completed quests: ${completedQuests.length}`);
      console.log(`Remaining quests: ${remainingQuests.length}`);
      console.log(`Characters needed: ${neededCharacters.length}`);

      const existingVoices = await elevenlabs.listVoices();

      console.log(`Existing voices: ${existingVoices.length}`);

      const unusedVoices = existingVoices.filter(
        (voice) => !neededCharacters.includes(voice.name)
      );

      console.log(`Unused voices: ${unusedVoices.length}`);

      if (dryRun || unusedVoices.length === 0) {
        return {
          neededCharacters,
          unusedVoices: [...unusedVoices],
          deletedVoices: [],
          failedDeletions: [],
          dryRun,
        };
      }

      const deletedVoices: string[] = [];
      const failedDeletions: { name: string; error: string }[] = [];

      for (const voice of unusedVoices) {
        try {
          await ctx.run(`delete-voice-${voice.voiceId}`, async () => {
            await elevenlabs.deleteVoice(voice.voiceId);
          });
          deletedVoices.push(voice.name);
          console.log(`Deleted: ${voice.name}`);
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : String(error);
          failedDeletions.push({ name: voice.name, error: errorMessage });
          console.error(`Failed to delete ${voice.name}: ${errorMessage}`);
        }
      }

      return {
        neededCharacters,
        unusedVoices: [...unusedVoices],
        deletedVoices,
        failedDeletions,
        dryRun,
      };
    },
  },
});
