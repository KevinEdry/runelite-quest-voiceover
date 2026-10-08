import * as wmill from "windmill-client";
import {
  createGitHubClient,
  createDeliveryAnnotator,
  removeSpecialCharacters,
  slugifyQuest,
  type CharacterInfo,
  type DialogLine,
  type GenerationTarget,
  type VoiceMap,
} from "./lib";

export interface PrepareQuestResult {
  featureBranch: string;
  targets: GenerationTarget[];
}

async function annotateDelivery(
  lines: DialogLine[],
  characters: CharacterInfo[]
): Promise<string[] | null> {
  let apiKey: string;
  try {
    apiKey = await wmill.getVariable("f/quest_voiceover/anthropic_api_key");
  } catch {
    console.log("No anthropic_api_key variable; generating without delivery tags");
    return null;
  }
  if (!apiKey) return null;

  const descriptions = Object.fromEntries((characters ?? []).map((c) => [c.name, c.description]));
  const cleaned = lines.map((line) => ({ character: line.character, line: removeSpecialCharacters(line.line.trim()) }));
  try {
    const annotated = await createDeliveryAnnotator(apiKey).annotate(cleaned, descriptions);
    console.log(`Annotated ${annotated.length} lines with v4 delivery tags`);
    return annotated;
  } catch (error) {
    console.warn(`Delivery annotation failed, generating plain: ${error instanceof Error ? error.message : String(error)}`);
    return null;
  }
}

export async function main(
  questName: string,
  lines: DialogLine[],
  voiceMap: VoiceMap,
  characters: CharacterInfo[],
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  featureBranch?: string,
  voiceActing = true
): Promise<PrepareQuestResult> {
  const delivery = voiceActing ? await annotateDelivery(lines, characters) : null;

  const targets: GenerationTarget[] = [];
  for (const [index, line] of lines.entries()) {
    const previousText = lines[index - 1]?.line;
    const nextText = lines[index + 1]?.line;
    const deliveryText = delivery?.[index];

    const cast =
      line.character === "Player" ? ["Player Male", "Player Female"] : [line.character];

    for (const character of cast) {
      const voiceId = voiceMap[character];
      if (!voiceId) {
        console.warn(`No voice for "${character}", skipping: "${line.line.substring(0, 50)}"`);
        continue;
      }
      targets.push({ questName, character, voiceId, text: line.line, deliveryText, previousText, nextText });
    }
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const now = new Date().toISOString();
  const timestamp = `${now.slice(0, 10).replace(/-/g, "")}-${now.slice(11, 19).replace(/:/g, "")}`;
  const branch = featureBranch || `voiceover-${slugifyQuest(questName)}-${timestamp}`;
  if (await github.branchExists(branch)) {
    console.log(`Feature branch ${branch} already exists, reusing it`);
  } else {
    await github.createBranch(branch, soundsBranch);
  }

  console.log(`${targets.length} targets for "${questName}" -> ${branch}`);
  return { featureBranch: branch, targets };
}
