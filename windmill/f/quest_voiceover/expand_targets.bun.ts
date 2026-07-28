import * as wmill from "windmill-client";
import {
  createGitHubClient,
  type DialogLine,
  type GenerationTarget,
  type VoiceMap,
} from "./lib";

export interface PrepareQuestResult {
  featureBranch: string;
  targets: GenerationTarget[];
}

function slugify(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 40);
}

export async function main(
  questName: string,
  lines: DialogLine[],
  voiceMap: VoiceMap,
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  featureBranch?: string
): Promise<PrepareQuestResult> {
  const targets: GenerationTarget[] = [];
  for (const [index, line] of lines.entries()) {
    const previousText = lines[index - 1]?.line;
    const nextText = lines[index + 1]?.line;

    const characters =
      line.character === "Player" ? ["Player Male", "Player Female"] : [line.character];

    for (const character of characters) {
      const voiceId = voiceMap[character];
      if (!voiceId) {
        console.warn(`No voice for "${character}", skipping: "${line.line.substring(0, 50)}"`);
        continue;
      }
      targets.push({ questName, character, voiceId, text: line.line, previousText, nextText });
    }
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const now = new Date().toISOString();
  const timestamp = `${now.slice(0, 10).replace(/-/g, "")}-${now.slice(11, 19).replace(/:/g, "")}`;
  const branch = featureBranch || `voiceover-${slugify(questName)}-${timestamp}`;
  if (await github.branchExists(branch)) {
    console.log(`Feature branch ${branch} already exists, reusing it`);
  } else {
    await github.createBranch(branch, soundsBranch);
  }

  console.log(`${targets.length} targets for "${questName}" -> ${branch}`);
  return { featureBranch: branch, targets };
}
