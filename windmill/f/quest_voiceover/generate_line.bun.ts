import * as wmill from "windmill-client";
import {
  createElevenLabsClient,
  createGitHubClient,
  generateDialogHash,
  type GenerationTarget,
  type LineResult,
} from "./lib";

// One job per generation target: skip if the audio already exists on the sounds
// branch, otherwise synthesise speech and upload it. Audio never leaves this job,
// so the binary buffer is never serialised across a flow step boundary. Failures
// are returned (not thrown) so a single bad line can't abort the whole quest.
export async function main(
  target: GenerationTarget,
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
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
      console.log(`[DRY RUN] Would upload ${hash}.mp3 (${target.character})`);
      return { ...base, uri: `${hash}.mp3`, status: "completed" };
    }

    const uri = await github.uploadAudioFile({
      audioData: speech.audioData,
      hash: speech.hash,
      questName: target.questName,
      character: target.character,
      soundsBranch,
    });
    return { ...base, uri, status: "completed" };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Failed for ${target.character}: ${message}`);
    return { ...base, uri: "", status: "failed", error: message };
  }
}
