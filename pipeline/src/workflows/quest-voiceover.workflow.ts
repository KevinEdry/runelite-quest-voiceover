import * as restate from "@restatedev/restate-sdk";
import type { QuestInput, QuestResult, LineResult } from "../types/index.js";
import { getElevenLabsClient } from "../clients/elevenlabs.client.js";
import { getGitHubClient } from "../clients/github.client.js";
import { getDatabaseProvider } from "../providers/database.provider.js";

const SOUNDS_BRANCH = "sounds";

export const questVoiceoverWorkflow = restate.workflow({
  name: "questVoiceover",
  handlers: {
    run: async (
      ctx: restate.WorkflowContext,
      input: QuestInput
    ): Promise<QuestResult> => {
      const { questName, lines, characters, playerVoiceId } = input;

      const elevenlabs = getElevenLabsClient();
      const github = getGitHubClient();
      const database = getDatabaseProvider();

      const voiceMap = await ctx.run("setup-voices", async () => {
        const result = await elevenlabs.setupVoicesForQuest(characters, playerVoiceId);
        return result.voiceMap;
      });

      const results: LineResult[] = [];

      for (const [index, line] of lines.entries()) {
        const previousLine = lines[index - 1];
        const nextLine = lines[index + 1];

        const voiceId = voiceMap[line.character];
        if (!voiceId) {
          results.push({
            hash: "",
            character: line.character,
            status: "skipped",
            error: "No voice available",
          });
          continue;
        }

        try {
          const hash = await ctx.run(
            `compute-hash-${index}`,
            async () => elevenlabs.computeHash(line.character, line.line)
          );

          const exists = await ctx.run(
            `check-exists-${index}`,
            async () => elevenlabs.checkAudioExists(hash, SOUNDS_BRANCH)
          );

          if (exists) {
            results.push({
              hash,
              character: line.character,
              status: "skipped",
            });
            continue;
          }

          const speechResult = await ctx.run(`generate-speech-${index}`, async () =>
            elevenlabs.generateSpeech({
              voiceId,
              text: line.line,
              character: line.character,
              previousText: previousLine?.line,
              nextText: nextLine?.line,
            })
          );

          const uri = await ctx.run(`upload-audio-${index}`, async () =>
            github.uploadAudioFile({
              audioData: speechResult.audioData,
              hash: speechResult.hash,
              questName,
              character: line.character,
              soundsBranch: SOUNDS_BRANCH,
            })
          );

          await ctx.run(`insert-dialog-${index}`, async () =>
            database.insertDialog({
              quest: questName,
              character: line.character,
              text: line.line,
              uri,
            })
          );

          results.push({
            hash: speechResult.hash,
            character: line.character,
            status: "completed",
          });

          console.log(
            `[${index + 1}/${lines.length}] Completed: ${line.character}`
          );
        } catch (error) {
          const errorMessage =
            error instanceof Error ? error.message : String(error);
          console.error(
            `[${index + 1}/${lines.length}] Failed for ${line.character}: ${errorMessage}`
          );

          results.push({
            hash: "",
            character: line.character,
            status: "failed",
            error: errorMessage,
          });
        }
      }

      await ctx.run("upload-database", async () => database.uploadDatabase());

      const completed = results.filter((r) => r.status === "completed").length;
      const skipped = results.filter((r) => r.status === "skipped").length;
      const failed = results.filter((r) => r.status === "failed").length;

      return {
        questName,
        totalLines: lines.length,
        completed,
        skipped,
        failed,
        results,
      };
    },

    getStatus: async (
      ctx: restate.WorkflowSharedContext
    ): Promise<{ status: string }> => {
      return { status: "running" };
    },
  },
});
