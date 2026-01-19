import * as restate from "@restatedev/restate-sdk";
import { getElevenLabsClient } from "../clients/elevenlabs.client.js";
import { getGitHubClient } from "../clients/github.client.js";
import { getDatabaseProvider } from "../providers/database.provider.js";

const SOUNDS_BRANCH = "sounds";

export interface BackfillFemaleVoicesInput {
  readonly playerFemaleVoiceId?: string;
  readonly limit?: number;
}

export interface BackfillLineResult {
  readonly quest: string;
  readonly text: string;
  readonly hash: string;
  readonly status: "completed" | "skipped" | "failed";
  readonly error?: string;
}

export interface BackfillFemaleVoicesResult {
  readonly totalMaleLines: number;
  readonly missingFemaleLines: number;
  readonly completed: number;
  readonly skipped: number;
  readonly failed: number;
  readonly results: BackfillLineResult[];
  readonly featureBranch: string;
}

export const backfillFemaleVoicesWorkflow = restate.workflow({
  name: "backfillFemaleVoices",
  handlers: {
    run: async (
      ctx: restate.WorkflowContext,
      input?: BackfillFemaleVoicesInput
    ): Promise<BackfillFemaleVoicesResult> => {
      const playerFemaleVoiceId = input?.playerFemaleVoiceId ?? process.env.PLAYER_FEMALE_VOICE_ID;
      const limit = input?.limit;

      if (!playerFemaleVoiceId) {
        throw new Error("Player female voice ID must be provided via input or PLAYER_FEMALE_VOICE_ID env var");
      }

      const elevenlabs = getElevenLabsClient();
      const github = getGitHubClient();
      const database = getDatabaseProvider();

      const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, "");
      const featureBranch = `backfill-female-voices-${timestamp}`;

      await ctx.run("create-feature-branch", async () => {
        const exists = await github.branchExists(featureBranch);
        if (!exists) {
          await github.createBranch(featureBranch, SOUNDS_BRANCH);
        } else {
          console.log(`Branch ${featureBranch} already exists, reusing it`);
        }
      });

      const maleLines = await ctx.run("get-male-lines", async () =>
        database.getDialogsByCharacter("Player Male")
      );

      console.log(`Found ${maleLines.length} Player Male lines`);

      const missingFemaleLines = await ctx.run("find-missing-female", async () => {
        const missing: { quest: string; character: string; text: string; uri: string }[] = [];
        for (const line of maleLines) {
          const exists = await database.checkDialogExists("Player Female", line.text);
          if (!exists) {
            missing.push(line);
          }
        }
        return missing;
      });

      console.log(`Found ${missingFemaleLines.length} missing Player Female lines`);

      const linesToProcess = limit ? missingFemaleLines.slice(0, limit) : missingFemaleLines;
      console.log(`Processing ${linesToProcess.length} lines${limit ? ` (limited to ${limit})` : ""}`);

      const results: BackfillLineResult[] = [];

      for (const [index, line] of linesToProcess.entries()) {
        const targetKey = `${index}-player-female`;

        try {
          const hash = await ctx.run(
            `compute-hash-${targetKey}`,
            async () => elevenlabs.computeHash("Player Female", line.text)
          );

          const exists = await ctx.run(
            `check-exists-${targetKey}`,
            async () => elevenlabs.checkAudioExists(hash, SOUNDS_BRANCH)
          );

          if (exists) {
            await ctx.run(`insert-existing-${targetKey}`, async () =>
              database.insertDialog({
                quest: line.quest,
                character: "Player Female",
                text: line.text,
                uri: `${hash}.mp3`,
              })
            );

            results.push({
              quest: line.quest,
              text: line.text,
              hash,
              status: "skipped",
            });
            console.log(`[${index + 1}/${linesToProcess.length}] Audio exists, added DB entry: "${line.text.substring(0, 50)}..."`);
            continue;
          }

          const speechResult = await ctx.run(`generate-speech-${targetKey}`, async () =>
            elevenlabs.generateSpeech({
              voiceId: playerFemaleVoiceId,
              text: line.text,
              character: "Player Female",
            })
          );

          const uri = await ctx.run(`upload-audio-${targetKey}`, async () =>
            github.uploadAudioFile({
              audioData: speechResult.audioData,
              hash: speechResult.hash,
              questName: line.quest,
              character: "Player Female",
              soundsBranch: featureBranch,
            })
          );

          await ctx.run(`insert-dialog-${targetKey}`, async () =>
            database.insertDialog({
              quest: line.quest,
              character: "Player Female",
              text: line.text,
              uri,
            })
          );

          results.push({
            quest: line.quest,
            text: line.text,
            hash: speechResult.hash,
            status: "completed",
          });

          console.log(
            `[${index + 1}/${linesToProcess.length}] Completed: "${line.text.substring(0, 50)}..."`
          );
        } catch (error) {
          const errorMessage =
            error instanceof Error ? error.message : String(error);
          console.error(
            `[${index + 1}/${linesToProcess.length}] Failed: ${errorMessage}`
          );

          results.push({
            quest: line.quest,
            text: line.text,
            hash: "",
            status: "failed",
            error: errorMessage,
          });
        }
      }

      const completed = results.filter((r) => r.status === "completed").length;
      const skipped = results.filter((r) => r.status === "skipped").length;
      const failed = results.filter((r) => r.status === "failed").length;

      return {
        totalMaleLines: maleLines.length,
        missingFemaleLines: missingFemaleLines.length,
        completed,
        skipped,
        failed,
        results,
        featureBranch,
      };
    },

    getStatus: async (
      ctx: restate.WorkflowSharedContext
    ): Promise<{ status: string }> => {
      return { status: "running" };
    },
  },
});
