import { ElevenLabsClient as ElevenLabsApi } from "@elevenlabs/elevenlabs-js";
import { withRetry } from "./retry";
import { generateDialogHash, removeSpecialCharacters } from "./text";

export interface CharacterInfo {
  name: string;
  description: string;
}

// Only "generated" voices are created by this pipeline and safe to delete; the rest are
// shared/default voices that must be preserved.
export type VoiceCategory = "generated" | "premade" | "professional" | "cloned" | "famous" | "high_quality";

export interface VoiceInfo {
  voiceId: string;
  name: string;
  category?: VoiceCategory;
}

export interface VoiceMap {
  [characterName: string]: string;
}

export interface GenerateSpeechInput {
  readonly voiceId: string;
  readonly text: string;
  readonly character: string;
  readonly previousText?: string;
  readonly nextText?: string;
}

export interface VoiceSample {
  filename: string;
  data: Buffer;
}

export interface ElevenLabsClient {
  readonly listVoices: () => Promise<readonly VoiceInfo[]>;
  readonly deleteVoice: (voiceId: string) => Promise<void>;
  readonly generateSpeech: (input: GenerateSpeechInput) => Promise<{ hash: string; audioData: Buffer }>;
  readonly setupVoicesForQuest: (
    characters: readonly CharacterInfo[],
    playerMaleVoiceId: string,
    playerFemaleVoiceId: string,
    resolveSamples?: (character: string) => Promise<VoiceSample[] | null>
  ) => Promise<VoiceMap>;
  readonly getSubscription: () => Promise<SubscriptionInfo>;
  // Instant Voice Clone — a "cloned" voice, which the cleanup flow never deletes.
  readonly createInstantVoiceClone: (name: string, samples: VoiceSample[], description?: string) => Promise<string>;
}

function matchCharacterToVoice(
  characterName: string,
  existingVoices: readonly VoiceInfo[]
): string | null {
  // Exact (case-insensitive) match only — pipeline voices are named exactly the character
  // name, and substring matching mis-assigns (e.g. "Ivan Strom" grabbing the "Ivan" voice).
  const normalizedCharacter = characterName.toLowerCase().trim();
  for (const voice of existingVoices) {
    if (voice.name.toLowerCase().trim() === normalizedCharacter) return voice.voiceId;
  }
  return null;
}

export interface VoiceSetupPlan {
  voiceMap: VoiceMap;
  toCreate: CharacterInfo[];
  missing: string[];
}

// Shared by setupVoicesForQuest and the pre-generation plan step so the "which voices
// exist vs. need creating" decision is identical in the preview and the execution.
export function planVoiceSetup(
  characters: readonly CharacterInfo[],
  existingVoices: readonly VoiceInfo[],
  playerMaleVoiceId: string,
  playerFemaleVoiceId: string
): VoiceSetupPlan {
  const voiceMap: VoiceMap = {
    "Player Male": playerMaleVoiceId,
    "Player Female": playerFemaleVoiceId,
  };
  const toCreate: CharacterInfo[] = [];
  const missing: string[] = [];

  for (const character of characters) {
    if (character.name === "Player") continue;
    const matched = matchCharacterToVoice(character.name, existingVoices);
    if (matched) voiceMap[character.name] = matched;
    else if (character.description) toCreate.push(character);
    else missing.push(character.name);
  }

  return { voiceMap, toCreate, missing };
}

export interface SubscriptionInfo {
  tier: string;
  characterCount: number;
  characterLimit: number;
  resetAtUnix: number;
}

export function createElevenLabsClient(apiKey: string): ElevenLabsClient {
  const client = new ElevenLabsApi({ apiKey });

  const listVoices = async (): Promise<readonly VoiceInfo[]> => {
    const response = await withRetry(() => client.voices.getAll());
    return response.voices.map((voice) => ({
      voiceId: voice.voiceId,
      name: voice.name ?? "",
      category: voice.category,
    }));
  };

  const deleteVoice = async (voiceId: string): Promise<void> => {
    try {
      await withRetry(() => client.voices.delete(voiceId));
    } catch (error: unknown) {
      const isNotFound =
        error &&
        typeof error === "object" &&
        "body" in error &&
        typeof error.body === "object" &&
        error.body !== null &&
        "detail" in error.body &&
        typeof error.body.detail === "object" &&
        error.body.detail !== null &&
        "status" in error.body.detail &&
        error.body.detail.status === "voice_does_not_exist";
      if (isNotFound) {
        console.log(`Voice ${voiceId} already deleted, skipping`);
        return;
      }
      throw error;
    }
  };

  const generateSpeech = async (
    input: GenerateSpeechInput
  ): Promise<{ hash: string; audioData: Buffer }> => {
    const hash = generateDialogHash(input.character, input.text);
    const cleanText = removeSpecialCharacters(input.text.trim());
    if (!cleanText) throw new Error(`Text is empty after cleaning: "${input.text}"`);

    const audioData = await withRetry(async () => {
      // eleven_v3 doesn't support previous_text/next_text yet, so the neighbour context on
      // the input isn't forwarded to the model.
      const audioStream = await client.textToSpeech.convert(input.voiceId, {
        text: cleanText,
        modelId: "eleven_v3",
        outputFormat: "mp3_44100_96",
        voiceSettings: {
          stability: 1.0,
          similarityBoost: 1.0,
          style: 0.0,
          useSpeakerBoost: true,
          speed: 1.5,
        },
      });

      const chunks: Buffer[] = [];
      for await (const chunk of audioStream) {
        chunks.push(Buffer.from(chunk));
      }
      return Buffer.concat(chunks);
    });

    return { hash, audioData };
  };

  const generateAndCreateVoice = async (character: CharacterInfo): Promise<string> => {
    const result = await withRetry(() =>
      client.textToVoice.design({
        voiceDescription: character.description,
        modelId: "eleven_ttv_v3",
        autoGenerateText: true,
      })
    );
    const preview = result.previews[0];
    if (!preview) throw new Error(`No voice preview generated for ${character.name}`);

    const voice = await withRetry(() =>
      client.textToVoice.create({
        voiceName: character.name,
        voiceDescription: character.description,
        generatedVoiceId: preview.generatedVoiceId,
      })
    );
    console.log(`Created voice ${voice.voiceId} for: ${character.name}`);
    return voice.voiceId;
  };

  const setupVoicesForQuest = async (
    characters: readonly CharacterInfo[],
    playerMaleVoiceId: string,
    playerFemaleVoiceId: string,
    resolveSamples?: (character: string) => Promise<VoiceSample[] | null>
  ): Promise<VoiceMap> => {
    const existingVoices = await listVoices();
    const plan = planVoiceSetup(characters, existingVoices, playerMaleVoiceId, playerFemaleVoiceId);
    const voiceMap: VoiceMap = { ...plan.voiceMap };

    for (const character of characters) {
      if (character.name === "Player" || voiceMap[character.name]) continue;

      // A character with existing audio but no voice was cloned away or deleted — rebuild it
      // via IVC from its own clips so it keeps continuity, rather than designing a new voice.
      const samples = resolveSamples ? await resolveSamples(character.name) : null;
      if (samples && samples.length > 0) {
        voiceMap[character.name] = await createInstantVoiceClone(
          character.name,
          samples,
          `Continuity clone of ${character.name} rebuilt from existing audio.`
        );
      } else if (character.description) {
        voiceMap[character.name] = await generateAndCreateVoice(character);
      } else {
        console.warn(`No voice, no audio, and no description for: ${character.name}, skipping`);
      }
    }

    return voiceMap;
  };

  const getSubscription = async (): Promise<SubscriptionInfo> => {
    const response = await fetch("https://api.elevenlabs.io/v1/user/subscription", {
      headers: { "xi-api-key": apiKey },
    });
    if (!response.ok) throw new Error(`Subscription fetch failed: ${response.status}`);
    const data: {
      tier: string;
      character_count: number;
      character_limit: number;
      next_character_count_reset_unix: number;
    } = await response.json();
    return {
      tier: data.tier,
      characterCount: data.character_count,
      characterLimit: data.character_limit,
      resetAtUnix: data.next_character_count_reset_unix,
    };
  };

  const createInstantVoiceClone = async (
    name: string,
    samples: VoiceSample[],
    description?: string
  ): Promise<string> => {
    const response = await withRetry(async () => {
      const form = new FormData();
      form.append("name", name);
      if (description) form.append("description", description);
      for (const sample of samples) {
        form.append("files", new Blob([sample.data], { type: "audio/mpeg" }), sample.filename);
      }
      const result = await fetch("https://api.elevenlabs.io/v1/voices/add", {
        method: "POST",
        headers: { "xi-api-key": apiKey },
        body: form,
      });
      if (!result.ok) throw new Error(`Instant voice clone failed: ${result.status} ${await result.text()}`);
      return result;
    });
    const data: { voice_id: string } = await response.json();
    console.log(`Cloned voice ${data.voice_id} for: ${name}`);
    return data.voice_id;
  };

  return { listVoices, deleteVoice, generateSpeech, setupVoicesForQuest, getSubscription, createInstantVoiceClone };
}
