import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import type { CharacterInfo } from "../tools/voice";
import type { DialogLine } from "../tools/types";

export interface LoadedTranscript {
  questName: string;
  lines: DialogLine[];
  characters: CharacterInfo[];
}

export async function main(
  quest: string,
  githubOwner: string,
  githubRepo: string,
  transcriptsBranch = "automations"
): Promise<LoadedTranscript> {
  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const found = await github.getFile(quest, transcriptsBranch);
  if (!found) throw new Error(`Transcript not found: ${quest} on ${transcriptsBranch}`);

  const data: { quest_name: string; lines: DialogLine[]; characters: CharacterInfo[] } = JSON.parse(
    found.content.toString("utf-8")
  );
  return { questName: data.quest_name, lines: data.lines, characters: data.characters };
}
