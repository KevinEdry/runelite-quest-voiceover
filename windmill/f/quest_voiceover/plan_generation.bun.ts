import * as wmill from "windmill-client";
import {
  createElevenLabsClient,
  planVoiceSetup,
  type CharacterInfo,
  type SubscriptionInfo,
} from "../tools/voice";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import { removeSpecialCharacters } from "../tools/text";
import type { DialogLine } from "../tools/types";

export interface CharacterVoicePlan {
  character: string;
  status: "matched" | "to_clone" | "to_create" | "missing";
  voiceId: string | null;
}

export interface QuestPlan {
  questName: string;
  characters: CharacterVoicePlan[];
  voicesToClone: string[];
  voicesToCreate: string[];
  totalLines: number;
  totalClips: number;
  estimatedCharacters: number;
  costPer1kCharacters: number;
  estimatedCostUsd: number;
  subscription: (SubscriptionInfo & { remaining: number }) | null;
}

export async function main(
  questName: string,
  lines: DialogLine[],
  characters: CharacterInfo[],
  githubOwner: string,
  githubRepo: string,
  costPer1kCharacters = 0.1,
  databaseBranch = "database",
  playerMaleVoiceId?: string,
  playerFemaleVoiceId?: string
): Promise<QuestPlan> {
  const maleVoiceId = playerMaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_male_voice_id"));
  const femaleVoiceId = playerFemaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_female_voice_id"));
  const elevenlabs = createElevenLabsClient(await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key"));

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });
  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });

  const existingVoices = await elevenlabs.listVoices();
  const plan = planVoiceSetup(characters, existingVoices, maleVoiceId, femaleVoiceId);
  const hasAudio = (name: string) => dialogs.getByCharacter(name).length > 0;

  const characterPlans: CharacterVoicePlan[] = characters
    .filter((character) => character.name !== "Player")
    .map((character): CharacterVoicePlan => {
      if (plan.voiceMap[character.name]) {
        return { character: character.name, status: "matched", voiceId: plan.voiceMap[character.name] };
      }
      if (hasAudio(character.name)) {
        return { character: character.name, status: "to_clone", voiceId: null };
      }
      if (character.description) {
        return { character: character.name, status: "to_create", voiceId: null };
      }
      return { character: character.name, status: "missing", voiceId: null };
    });

  const voiced = new Set(
    characterPlans.filter((c) => c.status !== "missing").map((c) => c.character)
  );

  let totalClips = 0;
  let estimatedCharacters = 0;
  for (const line of lines) {
    if (line.character !== "Player" && !voiced.has(line.character)) continue;
    const cleanText = removeSpecialCharacters(line.line.trim());
    if (!cleanText) continue;
    const clips = line.character === "Player" ? 2 : 1;
    totalClips += clips;
    estimatedCharacters += cleanText.length * clips;
  }

  const estimatedCostUsd = Math.round((estimatedCharacters / 1000) * costPer1kCharacters * 100) / 100;

  let subscription: (SubscriptionInfo & { remaining: number }) | null = null;
  try {
    const current = await elevenlabs.getSubscription();
    subscription = { ...current, remaining: current.characterLimit - current.characterCount };
  } catch (error) {
    console.warn(`Could not read subscription: ${error instanceof Error ? error.message : String(error)}`);
  }

  dialogs.database.close();
  dialogs.cleanup();

  return {
    questName,
    characters: characterPlans,
    voicesToClone: characterPlans.filter((c) => c.status === "to_clone").map((c) => c.character),
    voicesToCreate: characterPlans.filter((c) => c.status === "to_create").map((c) => c.character),
    totalLines: lines.length,
    totalClips,
    estimatedCharacters,
    costPer1kCharacters,
    estimatedCostUsd,
    subscription,
  };
}
