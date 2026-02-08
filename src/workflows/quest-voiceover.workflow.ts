import * as restate from "@restatedev/restate-sdk";
import type { QuestInput, QuestResult, LineResult, VoiceMap } from "../types/index.js";
import { getElevenLabsClient } from "../clients/elevenlabs.client.js";
import { getGitHubClient } from "../clients/github.client.js";
import { getDatabaseProvider } from "../providers/database.provider.js";

const SOUNDS_BRANCH = "sounds";

interface GenerationTarget {
  readonly character: string;
  readonly voiceId: string;
}

function getGenerationTargets(
  transcriptCharacter: string,
  voiceMap: VoiceMap
): readonly GenerationTarget[] {
  if (transcriptCharacter === "Player") {
    return [
      { character: "Player Male", voiceId: voiceMap["Player Male"] },
      { character: "Player Female", voiceId: voiceMap["Player Female"] },
    ];
  }
  return [{ character: transcriptCharacter, voiceId: voiceMap[transcriptCharacter] }];
}

export const questVoiceoverWorkflow = restate.workflow({
  name: "questVoiceover",
  handlers: {
    run: async (
      ctx: restate.WorkflowContext,
      input: QuestInput
    ): Promise<QuestResult> => {
      const { questName, lines, characters } = input;
      const playerMaleVoiceId = input.playerMaleVoiceId ?? process.env.PLAYER_MALE_VOICE_ID;
      const playerFemaleVoiceId = input.playerFemaleVoiceId ?? process.env.PLAYER_FEMALE_VOICE_ID;

      if (!playerMaleVoiceId || !playerFemaleVoiceId) {
        throw new Error("Player voice IDs must be provided via input or PLAYER_MALE_VOICE_ID/PLAYER_FEMALE_VOICE_ID env vars");
      }

      const elevenlabs = getElevenLabsClient();
      const github = getGitHubClient();
      const database = getDatabaseProvider();

      const voiceMap = await ctx.run("setup-voices", async () => {
        const result = await elevenlabs.setupVoicesForQuest(characters, playerMaleVoiceId, playerFemaleVoiceId);
        return result.voiceMap;
      });

      const results: LineResult[] = [];

      for (const [index, line] of lines.entries()) {
        const previousLine = lines[index - 1];
        const nextLine = lines[index + 1];

        const targets = getGenerationTargets(line.character, voiceMap);

        for (const target of targets) {
          if (!target.voiceId) {
            results.push({
              hash: "",
              character: target.character,
              status: "skipped",
              error: "No voice available",
            });
            continue;
          }

          const targetKey = `${index}-${target.character.replace(" ", "-").toLowerCase()}`;

          try {
            const hash = await ctx.run(
              `compute-hash-${targetKey}`,
              async () => elevenlabs.computeHash(target.character, line.line)
            );

            const exists = await ctx.run(
              `check-exists-${targetKey}`,
              async () => elevenlabs.checkAudioExists(hash, SOUNDS_BRANCH)
            );

            if (exists) {
              results.push({
                hash,
                character: target.character,
                status: "skipped",
              });
              continue;
            }

            const speechResult = await ctx.run(`generate-speech-${targetKey}`, async () =>
              elevenlabs.generateSpeech({
                voiceId: target.voiceId,
                text: line.line,
                character: target.character,
                previousText: previousLine?.line,
                nextText: nextLine?.line,
              })
            );

            const uri = await ctx.run(`upload-audio-${targetKey}`, async () =>
              github.uploadAudioFile({
                audioData: speechResult.audioData,
                hash: speechResult.hash,
                questName,
                character: target.character,
                soundsBranch: SOUNDS_BRANCH,
              })
            );

            await ctx.run(`insert-dialog-${targetKey}`, async () =>
              database.insertDialog({
                quest: questName,
                character: target.character,
                text: line.line,
                uri,
              })
            );

            results.push({
              hash: speechResult.hash,
              character: target.character,
              status: "completed",
            });

            console.log(
              `[${index + 1}/${lines.length}] Completed: ${target.character}`
            );
          } catch (error) {
            const errorMessage =
              error instanceof Error ? error.message : String(error);
            console.error(
              `[${index + 1}/${lines.length}] Failed for ${target.character}: ${errorMessage}`
            );

            results.push({
              hash: "",
              character: target.character,
              status: "failed",
              error: errorMessage,
            });
          }
        }
      }

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
