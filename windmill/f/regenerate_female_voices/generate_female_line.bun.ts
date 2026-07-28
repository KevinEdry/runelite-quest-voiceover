import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { createElevenLabsClient } from "../tools/voice";
import { generateDialogHash } from "../tools/text";
import type { GenerationTarget, LineResult } from "../tools/types";

export interface RegenLineResult extends LineResult {
  quest: string;
}

// Unlike the main flow, this does not skip lines already on sounds — the whole point is
// to overwrite the old professional-clone audio. On resume, a feature-branch file whose
// blob differs from the sounds baseline (or is absent from sounds) was written by us, so
// it's skipped and the run continues without re-billing ElevenLabs.
export async function main(
  target: GenerationTarget,
  githubOwner: string,
  githubRepo: string,
  featureBranch: string,
  soundsBranch = "sounds",
  dryRun = false,
  resume = false
): Promise<RegenLineResult> {
  const hash = generateDialogHash(target.character, target.text);
  const base = { quest: target.questName, character: target.character, text: target.text, hash };

  try {
    const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
    const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

    if (resume && !dryRun) {
      const featureSha = await github.getFileSha(`${hash}.mp3`, featureBranch);
      if (featureSha) {
        const soundsSha = await github.getFileSha(`${hash}.mp3`, soundsBranch);
        if (soundsSha === null || soundsSha !== featureSha) {
          console.log(`[RESUME] already regenerated ${hash}.mp3 (${target.character}), skipping`);
          return { ...base, uri: `${hash}.mp3`, status: "skipped" };
        }
      }
    }

    const apiKey = await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key");
    const elevenlabs = createElevenLabsClient(apiKey);
    const speech = await elevenlabs.generateSpeech({
      voiceId: target.voiceId,
      text: target.text,
      character: target.character,
    });

    if (dryRun) {
      console.log(`[DRY RUN] Would commit ${hash}.mp3 (${target.character}) to ${featureBranch}`);
      return { ...base, uri: `${hash}.mp3`, status: "completed" };
    }

    const uri = await github.uploadAudioFile({
      audioData: speech.audioData,
      hash: speech.hash,
      questName: target.questName,
      character: target.character,
      soundsBranch: featureBranch,
    });
    return { ...base, uri, status: "completed" };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Failed for ${target.character}: ${message}`);
    return { ...base, uri: "", status: "failed", error: message };
  }
}
