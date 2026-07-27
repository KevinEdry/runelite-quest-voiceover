import * as wmill from "windmill-client";
import { createElevenLabsClient, type CharacterInfo, type VoiceMap } from "./lib";

export async function main(
  characters: CharacterInfo[],
  playerMaleVoiceId?: string,
  playerFemaleVoiceId?: string
): Promise<VoiceMap> {
  // Fall back to stored variables so the player voice IDs can be configured once
  // instead of passed on every run (mirrors the original PLAYER_*_VOICE_ID env vars).
  const maleVoiceId = playerMaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_male_voice_id"));
  const femaleVoiceId =
    playerFemaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_female_voice_id"));

  if (!maleVoiceId || !femaleVoiceId) {
    throw new Error(
      "Player voice IDs required: pass them as inputs or set the f/quest_voiceover/player_male_voice_id and player_female_voice_id variables"
    );
  }

  const apiKey = await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key");
  const elevenlabs = createElevenLabsClient(apiKey);
  return elevenlabs.setupVoicesForQuest(characters, maleVoiceId, femaleVoiceId);
}
