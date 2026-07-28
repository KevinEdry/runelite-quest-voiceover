import * as wmill from "windmill-client";
import { createElevenLabsClient, type CharacterInfo, type VoiceMap } from "../tools/voice";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import { collectVoiceSamples } from "../tools/samples";

export async function main(
  characters: CharacterInfo[],
  githubOwner: string,
  githubRepo: string,
  playerMaleVoiceId?: string,
  playerFemaleVoiceId?: string,
  soundsBranch = "sounds",
  databaseBranch = "database",
  targetSeconds = 120,
  maxClips = 60
): Promise<VoiceMap> {
  const maleVoiceId = playerMaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_male_voice_id"));
  const femaleVoiceId =
    playerFemaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_female_voice_id"));
  if (!maleVoiceId || !femaleVoiceId) {
    throw new Error(
      "Player voice IDs required: pass them as inputs or set the f/quest_voiceover/player_male_voice_id and player_female_voice_id variables"
    );
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });
  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });

  const resolveSamples = async (character: string) => {
    const uris = dialogs.getClipUris(character, maxClips);
    if (uris.length === 0) return null;
    return collectVoiceSamples(github, uris, soundsBranch, targetSeconds);
  };

  const elevenlabs = createElevenLabsClient(await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key"));
  const voiceMap = await elevenlabs.setupVoicesForQuest(characters, maleVoiceId, femaleVoiceId, resolveSamples);

  dialogs.database.close();
  dialogs.cleanup();
  return voiceMap;
}
