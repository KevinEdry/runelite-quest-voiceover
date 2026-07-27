import { ElevenLabsClient as ElevenLabsApi } from "@elevenlabs/elevenlabs-js";
import { withRetry } from "./retry";
import { generateDialogHash, removeSpecialCharacters } from "./text";

export interface CharacterInfo {
  name: string;
  description: string;
}

export interface VoiceInfo {
  voiceId: string;
  name: string;
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

export interface ElevenLabsClient {
  readonly listVoices: () => Promise<readonly VoiceInfo[]>;
  readonly deleteVoice: (voiceId: string) => Promise<void>;
  readonly generateSpeech: (input: GenerateSpeechInput) => Promise<{ hash: string; audioData: Buffer }>;
  readonly setupVoicesForQuest: (
    characters: readonly CharacterInfo[],
    playerMaleVoiceId: string,
    playerFemaleVoiceId: string
  ) => Promise<VoiceMap>;
}

function matchCharacterToVoice(
  characterName: string,
  existingVoices: readonly VoiceInfo[]
): string | null {
  const normalizedCharacter = characterName.toLowerCase().trim();
  for (const voice of existingVoices) {
    const normalizedVoiceName = voice.name.toLowerCase().trim();
    if (normalizedVoiceName === normalizedCharacter) return voice.voiceId;
    if (
      normalizedVoiceName.includes(normalizedCharacter) ||
      normalizedCharacter.includes(normalizedVoiceName)
    ) {
      return voice.voiceId;
    }
  }
  return null;
}

export function createElevenLabsClient(apiKey: string): ElevenLabsClient {
  const client = new ElevenLabsApi({ apiKey });

  const listVoices = async (): Promise<readonly VoiceInfo[]> => {
    const response = await withRetry(() => client.voices.getAll());
    return response.voices.map((voice) => ({ voiceId: voice.voiceId, name: voice.name ?? "" }));
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
        previousText: input.previousText ? removeSpecialCharacters(input.previousText) : undefined,
        nextText: input.nextText ? removeSpecialCharacters(input.nextText) : undefined,
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
      client.textToVoice.design({ voiceDescription: character.description, modelId: "eleven_ttv_v3" })
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
    playerFemaleVoiceId: string
  ): Promise<VoiceMap> => {
    const existingVoices = await listVoices();
    console.log(`Found ${existingVoices.length} existing voices`);

    const voiceMap: VoiceMap = {
      "Player Male": playerMaleVoiceId,
      "Player Female": playerFemaleVoiceId,
    };
    const mutableExistingVoices = [...existingVoices];

    for (const character of characters) {
      if (character.name === "Player") continue;

      const matchedVoiceId = matchCharacterToVoice(character.name, mutableExistingVoices);
      if (matchedVoiceId) {
        console.log(`Matched ${character.name} to existing voice: ${matchedVoiceId}`);
        voiceMap[character.name] = matchedVoiceId;
      } else if (character.description) {
        const newVoiceId = await generateAndCreateVoice(character);
        voiceMap[character.name] = newVoiceId;
        mutableExistingVoices.push({ voiceId: newVoiceId, name: character.name });
      } else {
        console.warn(`No voice match and no description for: ${character.name}, skipping`);
      }
    }

    return voiceMap;
  };

  return { listVoices, deleteVoice, generateSpeech, setupVoicesForQuest };
}
