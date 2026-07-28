import * as wmill from "windmill-client";
import {
  createElevenLabsClient,
  createGitHubClient,
  generateDialogHash,
  type GenerationTarget,
  type LineResult,
} from "./lib";

export async function main(
  target: GenerationTarget,
  githubOwner: string,
  githubRepo: string,
  featureBranch: string,
  soundsBranch = "sounds",
  resume = false,
  dryRun = false
): Promise<LineResult> {
  const hash = generateDialogHash(target.character, target.text);
  const base = { character: target.character, text: target.text, hash };

  try {
    const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
    const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

    if (await github.checkAudioFileExists(hash, soundsBranch)) {
      return { ...base, uri: `${hash}.mp3`, status: "skipped" };
    }

    // On the feature branch but not on sounds means we committed it on a prior attempt.
    if (resume && (await github.checkAudioFileExists(hash, featureBranch))) {
      return { ...base, uri: `${hash}.mp3`, status: "skipped" };
    }

    const apiKey = await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key");
    const elevenlabs = createElevenLabsClient(apiKey);
    const speech = await elevenlabs.generateSpeech({
      voiceId: target.voiceId,
      text: target.text,
      character: target.character,
      previousText: target.previousText,
      nextText: target.nextText,
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
