import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import { createElevenLabsClient } from "../tools/voice";

export interface CloneTarget {
  character: string;
  uris: string[];
}

export interface PlanVoiceClonesResult {
  targets: CloneTarget[];
  skipped: { character: string; reason: string }[];
}

export async function main(
  characters: string[],
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  databaseBranch = "database",
  maxCandidateClips = 60
): Promise<PlanVoiceClonesResult> {
  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });
  const elevenlabs = createElevenLabsClient(await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key"));

  const existing = new Set((await elevenlabs.listVoices()).map((voice) => voice.name.toLowerCase().trim()));
  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });

  const targets: CloneTarget[] = [];
  const skipped: { character: string; reason: string }[] = [];
  for (const character of characters) {
    if (existing.has(character.toLowerCase().trim())) {
      skipped.push({ character, reason: "voice already exists" });
      continue;
    }
    const uris = dialogs.getClipUris(character, maxCandidateClips);
    if (uris.length === 0) skipped.push({ character, reason: "no existing audio in the database" });
    else targets.push({ character, uris });
  }
  dialogs.database.close();
  dialogs.cleanup();

  console.log(`${targets.length} to clone, ${skipped.length} skipped`);
  return { targets, skipped };
}
