import * as restate from "@restatedev/restate-sdk";
import { getElevenLabsClient } from "@/clients/elevenlabs.client.js";
import { getGitHubClient } from "@/clients/github.client.js";
import { getDatabaseProvider } from "@/providers/database.provider.js";
import { processLine } from "./process-line.js";
import type {
  BackfillFemaleVoicesInput,
  BackfillFemaleVoicesResult,
  BackfillLineResult,
} from "./types.js";

export type { BackfillFemaleVoicesInput, BackfillLineResult, BackfillFemaleVoicesResult };

export const backfillFemaleVoicesWorkflow = restate.workflow({
  name: "backfillFemaleVoices",
  handlers: {
    run: async (
      ctx: restate.WorkflowContext,
      input?: BackfillFemaleVoicesInput
    ): Promise<BackfillFemaleVoicesResult> => {
      const playerFemaleVoiceId = input?.playerFemaleVoiceId ?? process.env.PLAYER_FEMALE_VOICE_ID;
      const limit = input?.limit;
      const forceRegenerate = input?.forceRegenerate ?? false;

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
          await github.createBranch(featureBranch, "sounds");
        } else {
          console.log(`Branch ${featureBranch} already exists, reusing it`);
        }
      });

      const maleLines = await ctx.run("get-male-lines", async () =>
        database.getDialogsByCharacter("Player Male")
      );

      console.log(`Found ${maleLines.length} Player Male lines`);

      const linesToProcess = forceRegenerate
        ? maleLines
        : await ctx.run("find-missing-female", async () => {
            const missing: { quest: string; character: string; text: string; uri: string }[] = [];
            for (const line of maleLines) {
              const exists = await database.checkDialogExists("Player Female", line.text);
              if (!exists) {
                missing.push(line);
              }
            }
            return missing;
          });

      console.log(
        forceRegenerate
          ? `Force regenerating all ${linesToProcess.length} Player Female lines`
          : `Found ${linesToProcess.length} missing Player Female lines`
      );

      const limited = limit ? linesToProcess.slice(0, limit) : linesToProcess;
      console.log(`Processing ${limited.length} lines${limit ? ` (limited to ${limit})` : ""}`);

      const results: BackfillLineResult[] = [];

      for (const [index, line] of limited.entries()) {
        const targetKey = `${index}-player-female`;

        try {
          const result = await processLine(
            { ctx, elevenlabs, github, database },
            { line, targetKey, playerFemaleVoiceId, featureBranch, forceRegenerate }
          );
          results.push(result);
          console.log(
            `[${index + 1}/${limited.length}] ${result.status === "skipped" ? "Audio exists, added DB entry" : "Completed"}: "${line.text.substring(0, 50)}..."`
          );
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : String(error);
          console.error(`[${index + 1}/${limited.length}] Failed: ${errorMessage}`);
          results.push({ quest: line.quest, text: line.text, hash: "", status: "failed", error: errorMessage });
        }
      }

      const completed = results.filter((r) => r.status === "completed").length;
      const skipped = results.filter((r) => r.status === "skipped").length;
      const failed = results.filter((r) => r.status === "failed").length;

      return {
        totalMaleLines: maleLines.length,
        missingFemaleLines: linesToProcess.length,
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
