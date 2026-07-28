import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import { createElevenLabsClient, type VoiceInfo } from "../tools/voice";

interface TranscriptCharacters {
  quest_name: string;
  characters: { name: string; description?: string }[];
}

export interface AnalyzeUnusedVoicesResult {
  neededCharacters: string[];
  completedQuests: number;
  remainingQuests: number;
  totalVoices: number;
  protectedVoices: number;
  unused: VoiceInfo[];
}

export async function main(
  githubOwner: string,
  githubRepo: string,
  transcriptsBranch = "automations",
  transcriptsPath = "transcripts",
  databaseBranch = "database"
): Promise<AnalyzeUnusedVoicesResult> {
  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });
  const completedQuests = new Set(dialogs.getQuests());
  dialogs.database.close();
  dialogs.cleanup();

  // A voice is needed only if its character appears in a quest not yet voiced — anything
  // that only appears in already-completed quests won't be generated again.
  const needed = new Set<string>();
  let remainingQuests = 0;
  for (const file of await github.listDirectory(transcriptsPath, transcriptsBranch)) {
    if (!file.endsWith(".json")) continue;
    const found = await github.getFile(file, transcriptsBranch);
    if (!found) continue;
    const transcript = JSON.parse(found.content.toString("utf-8")) as TranscriptCharacters;
    if (completedQuests.has(transcript.quest_name)) continue;
    remainingQuests++;
    for (const character of transcript.characters) needed.add(character.name);
  }

  const playerMaleVoiceId = await wmill.getVariable("f/quest_voiceover/player_male_voice_id");
  const playerFemaleVoiceId = await wmill.getVariable("f/quest_voiceover/player_female_voice_id");
  const protectedVoiceIds = new Set([playerMaleVoiceId, playerFemaleVoiceId].filter(Boolean));

  const voices = await createElevenLabsClient(
    await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key")
  ).listVoices();

  const unused = voices.filter(
    (voice) =>
      voice.category === "generated" &&
      !protectedVoiceIds.has(voice.voiceId) &&
      !needed.has(voice.name)
  );

  console.log(
    `${completedQuests.size} completed / ${remainingQuests} remaining quests, ` +
      `${needed.size} characters needed, ${voices.length} voices, ${unused.length} unused`
  );

  return {
    neededCharacters: [...needed].sort(),
    completedQuests: completedQuests.size,
    remainingQuests,
    totalVoices: voices.length,
    protectedVoices: voices.length - unused.length,
    unused,
  };
}
