import * as wmill from "windmill-client";
import { createElevenLabsClient, type VoiceInfo } from "../tools/voice";

export interface DeleteVoiceResult {
  voiceId: string;
  name: string;
  status: "deleted" | "failed";
  error?: string;
}

export async function main(voice: VoiceInfo): Promise<DeleteVoiceResult> {
  const base = { voiceId: voice.voiceId, name: voice.name };
  try {
    const apiKey = await wmill.getVariable("f/quest_voiceover/elevenlabs_api_key");
    await createElevenLabsClient(apiKey).deleteVoice(voice.voiceId);
    console.log(`Deleted ${voice.name} (${voice.voiceId})`);
    return { ...base, status: "deleted" };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Failed to delete ${voice.name}: ${message}`);
    return { ...base, status: "failed", error: message };
  }
}
