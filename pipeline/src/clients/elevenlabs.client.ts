import { ElevenLabsClient } from "@elevenlabs/elevenlabs-js";
import { createRateLimiterRegistry, withRateLimit } from "./base.client.js";
import { getGitHubClient } from "./github.client.js";
import { removeSpecialCharacters } from "../utilities/text.util.js";
import { generateDialogHash } from "../utilities/hash.util.js";
import type { RateLimitConfig, VoiceInfo, CharacterInfo, VoiceMap } from "../types/index.js";

const RATE_LIMIT_CONFIGS: Record<string, RateLimitConfig> = {
  voiceDesign: { tokens: 10, refillRate: 10, intervalMs: 60000 },
  voiceCreate: { tokens: 10, refillRate: 10, intervalMs: 60000 },
  tts: { tokens: 60, refillRate: 60, intervalMs: 60000 },
  listVoices: { tokens: 100, refillRate: 100, intervalMs: 60000 },
};

export interface GenerateSpeechResult {
  readonly hash: string;
  readonly audioData: Buffer;
}

export interface VoiceSetupResult {
  readonly voiceMap: VoiceMap;
  readonly createdVoices: readonly string[];
  readonly matchedVoices: readonly string[];
}

export interface ElevenLabsClientInstance {
  readonly listVoices: () => Promise<readonly VoiceInfo[]>;
  readonly generateVoicePreview: (description: string) => Promise<{ generatedVoiceId: string; audioData: Buffer }>;
  readonly createVoiceFromPreview: (generatedVoiceId: string, name: string, description: string) => Promise<string>;
  readonly deleteVoice: (voiceId: string) => Promise<void>;
  readonly generateSpeech: (input: GenerateSpeechInput) => Promise<GenerateSpeechResult>;
  readonly computeHash: (character: string, text: string) => string;
  readonly checkAudioExists: (hash: string, soundsBranch: string) => Promise<boolean>;
  readonly matchCharacterToVoice: (characterName: string, existingVoices: readonly VoiceInfo[]) => string | null;
  readonly generateAndCreateVoice: (character: CharacterInfo) => Promise<string>;
  readonly setupVoicesForQuest: (characters: readonly CharacterInfo[], playerMaleVoiceId: string, playerFemaleVoiceId: string) => Promise<VoiceSetupResult>;
}

export interface GenerateSpeechInput {
  readonly voiceId: string;
  readonly text: string;
  readonly character: string;
  readonly previousText?: string;
  readonly nextText?: string;
}

export function createElevenLabsClient(apiKey: string): ElevenLabsClientInstance {
  const client = new ElevenLabsClient({ apiKey });
  const getRateLimiter = createRateLimiterRegistry(RATE_LIMIT_CONFIGS);

  const listVoices = async (): Promise<readonly VoiceInfo[]> => {
    return withRateLimit(getRateLimiter("listVoices"), async () => {
      const response = await client.voices.getAll();
      return response.voices.map((voice) => ({
        voiceId: voice.voiceId,
        name: voice.name ?? "",
      }));
    });
  };

  const generateVoicePreview = async (
    description: string
  ): Promise<{ generatedVoiceId: string; audioData: Buffer }> => {
    return withRateLimit(getRateLimiter("voiceDesign"), async () => {
      const result = await client.textToVoice.design({
        voiceDescription: description,
        modelId: "eleven_ttv_v3",
      });

      const preview = result.previews[0];
      if (!preview) {
        throw new Error("No voice preview generated");
      }

      return {
        generatedVoiceId: preview.generatedVoiceId,
        audioData: Buffer.from(preview.audioBase64, "base64"),
      };
    });
  };

  const createVoiceFromPreview = async (
    generatedVoiceId: string,
    name: string,
    description: string
  ): Promise<string> => {
    return withRateLimit(getRateLimiter("voiceCreate"), async () => {
      const voice = await client.textToVoice.create({
        voiceName: name,
        voiceDescription: description,
        generatedVoiceId: generatedVoiceId,
      });

      return voice.voiceId;
    });
  };

  const deleteVoice = async (voiceId: string): Promise<void> => {
    try {
      await withRateLimit(getRateLimiter("voiceCreate"), async () => {
        await client.voices.delete(voiceId);
      });
    } catch (error: unknown) {
      const isNotFound =
        error &&
        typeof error === "object" &&
        "statusCode" in error &&
        error.statusCode === 400 &&
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

  const generateSpeechAudio = async (
    voiceId: string,
    text: string,
    previousText?: string,
    nextText?: string
  ): Promise<Buffer> => {
    return withRateLimit(getRateLimiter("tts"), async () => {
      const cleanText = removeSpecialCharacters(text.trim());
      const cleanPreviousText = previousText
        ? removeSpecialCharacters(previousText)
        : undefined;
      const cleanNextText = nextText
        ? removeSpecialCharacters(nextText)
        : undefined;

      if (!cleanText) {
        throw new Error(`Text is empty after cleaning: "${text}"`);
      }

      console.log(`Generating speech for voice ${voiceId}: "${cleanText.substring(0, 50)}..."`);

      const options: Parameters<typeof client.textToSpeech.convert>[1] = {
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
        previousText: cleanPreviousText,
        nextText: cleanNextText,
      };

      const audioStream = await client.textToSpeech.convert(voiceId, options);

      const chunks: Buffer[] = [];
      for await (const chunk of audioStream) {
        chunks.push(Buffer.from(chunk));
      }

      return Buffer.concat(chunks);
    });
  };

  const computeHash = (character: string, text: string): string => {
    return generateDialogHash(character, text);
  };

  const generateSpeech = async (input: GenerateSpeechInput): Promise<GenerateSpeechResult> => {
    const hash = computeHash(input.character, input.text);

    const audioData = await generateSpeechAudio(
      input.voiceId,
      input.text,
      input.previousText,
      input.nextText
    );

    return { hash, audioData };
  };

  const checkAudioExists = async (hash: string, soundsBranch: string): Promise<boolean> => {
    const github = getGitHubClient();
    return github.checkAudioFileExists(hash, soundsBranch);
  };

  const matchCharacterToVoice = (
    characterName: string,
    existingVoices: readonly VoiceInfo[]
  ): string | null => {
    const normalizedCharacter = characterName.toLowerCase().trim();

    for (const voice of existingVoices) {
      const normalizedVoiceName = voice.name.toLowerCase().trim();

      if (normalizedVoiceName === normalizedCharacter) {
        return voice.voiceId;
      }

      if (
        normalizedVoiceName.includes(normalizedCharacter) ||
        normalizedCharacter.includes(normalizedVoiceName)
      ) {
        return voice.voiceId;
      }
    }

    return null;
  };

  const generateAndCreateVoice = async (character: CharacterInfo): Promise<string> => {
    console.log(`Generating voice preview for: ${character.name}`);
    const preview = await generateVoicePreview(character.description);

    console.log(`Creating voice from preview for: ${character.name}`);
    const voiceId = await createVoiceFromPreview(
      preview.generatedVoiceId,
      character.name,
      character.description
    );

    console.log(`Created voice ${voiceId} for: ${character.name}`);
    return voiceId;
  };

  const setupVoicesForQuest = async (
    characters: readonly CharacterInfo[],
    playerMaleVoiceId: string,
    playerFemaleVoiceId: string
  ): Promise<VoiceSetupResult> => {
    const existingVoices = await listVoices();
    console.log(`Found ${existingVoices.length} existing voices`);

    const voiceMap: VoiceMap = {
      "Player Male": playerMaleVoiceId,
      "Player Female": playerFemaleVoiceId,
    };
    const createdVoices: string[] = [];
    const matchedVoices: string[] = ["Player Male", "Player Female"];
    const mutableExistingVoices = [...existingVoices];

    for (const character of characters) {
      if (character.name === "Player") {
        continue;
      }

      const matchedVoiceId = matchCharacterToVoice(character.name, mutableExistingVoices);

      if (matchedVoiceId) {
        console.log(`Matched ${character.name} to existing voice: ${matchedVoiceId}`);
        voiceMap[character.name] = matchedVoiceId;
        matchedVoices.push(character.name);
      } else if (character.description) {
        const newVoiceId = await generateAndCreateVoice(character);
        voiceMap[character.name] = newVoiceId;
        createdVoices.push(character.name);
        mutableExistingVoices.push({ voiceId: newVoiceId, name: character.name });
      } else {
        console.warn(
          `No voice match and no description for: ${character.name}, skipping voice creation`
        );
      }
    }

    return { voiceMap, createdVoices, matchedVoices };
  };

  return {
    listVoices,
    generateVoicePreview,
    createVoiceFromPreview,
    deleteVoice,
    generateSpeech,
    computeHash,
    checkAudioExists,
    matchCharacterToVoice,
    generateAndCreateVoice,
    setupVoicesForQuest,
  };
}

let elevenLabsClientInstance: ElevenLabsClientInstance | null = null;

export function getElevenLabsClient(): ElevenLabsClientInstance {
  if (elevenLabsClientInstance) {
    return elevenLabsClientInstance;
  }

  const apiKey = process.env.ELEVENLABS_API_KEY;
  if (!apiKey) {
    throw new Error("ELEVENLABS_API_KEY environment variable is required");
  }

  elevenLabsClientInstance = createElevenLabsClient(apiKey);
  return elevenLabsClientInstance;
}
