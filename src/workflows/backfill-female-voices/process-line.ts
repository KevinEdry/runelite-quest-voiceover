import type { WorkflowContext } from "@restatedev/restate-sdk";
import type { ElevenLabsClientInstance } from "@/clients/elevenlabs.client.js";
import type { GitHubClient } from "@/clients/github.client.js";
import type { DatabaseProvider } from "@/providers/database.provider.js";
import type { BackfillLineResult } from "./types.js";

interface ProcessLineParams {
  readonly line: { readonly quest: string; readonly text: string };
  readonly targetKey: string;
  readonly playerFemaleVoiceId: string;
  readonly featureBranch: string;
  readonly forceRegenerate: boolean;
}

interface ProcessLineContext {
  readonly ctx: WorkflowContext;
  readonly elevenlabs: ElevenLabsClientInstance;
  readonly github: GitHubClient;
  readonly database: DatabaseProvider;
}

export async function processLine(
  { ctx, elevenlabs, github, database }: ProcessLineContext,
  { line, targetKey, playerFemaleVoiceId, featureBranch, forceRegenerate }: ProcessLineParams
): Promise<BackfillLineResult> {
  const hash = await ctx.run(
    `compute-hash-${targetKey}`,
    async () => elevenlabs.computeHash("Player Female", line.text)
  );

  if (!forceRegenerate) {
    const exists = await ctx.run(
      `check-exists-${targetKey}`,
      async () => elevenlabs.checkAudioExists(hash, "sounds")
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

      return { quest: line.quest, text: line.text, hash, status: "skipped" };
    }
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

  return { quest: line.quest, text: line.text, hash: speechResult.hash, status: "completed" };
}
