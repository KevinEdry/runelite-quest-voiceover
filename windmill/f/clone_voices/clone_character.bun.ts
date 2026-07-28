import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { createElevenLabsClient } from "../tools/voice";
import { collectVoiceSamples } from "../tools/samples";
import type { CloneTarget } from "./plan_voice_clones";

export interface CloneResult {
  character: string;
  voiceId: string | null;
  seconds: number;
  samples: number;
  status: "cloned" | "skipped" | "failed";
  error?: string;
}

export async function main(
  target: CloneTarget,
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  targetSeconds = 120,
  dryRun = false
): Promise<CloneResult> {
  try {
    const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
    const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

    const samples = await collectVoiceSamples(github, target.uris, soundsBranch, targetSeconds);
    const seconds = samples.reduce((total, sample) => total + sample.data.length / 12000, 0);
    const base = { character: target.character, seconds: Math.round(seconds), samples: samples.length };
    if (samples.length === 0) {
      return { ...base, voiceId: null, status: "failed", error: "no clips could be downloaded" };
    }

    if (dryRun) {
      console.log(`[DRY RUN] Would clone ${target.character} from ${samples.length} clips (~${Math.round(seconds)}s)`);
      return { ...base, voiceId: null, status: "skipped" };
    }

    const elevenlabs = createElevenLabsClient(await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key"));
    const voiceId = await elevenlabs.createInstantVoiceClone(
      target.character,
      samples,
      `Continuity clone of ${target.character} rebuilt from existing audio.`
    );
    return { ...base, voiceId, status: "cloned" };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Failed to clone ${target.character}: ${message}`);
    return { character: target.character, voiceId: null, seconds: 0, samples: 0, status: "failed", error: message };
  }
}
