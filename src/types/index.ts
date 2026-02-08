export interface DialogLine {
  character: string;
  line: string;
}

export interface CharacterInfo {
  name: string;
  description: string;
}

export interface QuestTranscript {
  branch: string;
  quest_name: string;
  lines: DialogLine[];
  characters: CharacterInfo[];
}

export interface VoiceInfo {
  voiceId: string;
  name: string;
}

export interface VoiceMap {
  [characterName: string]: string;
}

export interface QuestInput {
  questName: string;
  branch: string;
  lines: DialogLine[];
  characters: CharacterInfo[];
  playerMaleVoiceId?: string;
  playerFemaleVoiceId?: string;
}

export interface VoiceSetupInput {
  characters: CharacterInfo[];
  playerMaleVoiceId: string;
  playerFemaleVoiceId: string;
}

export interface VoiceSetupResult {
  voiceMap: VoiceMap;
  createdVoices: string[];
  matchedVoices: string[];
}

export interface GenerateSpeechInput {
  voiceId: string;
  text: string;
  character: string;
  hash: string;
}

export interface GenerateSpeechResult {
  hash: string;
  audioData: Buffer;
}

export interface UploadAudioInput {
  audioData: Buffer;
  hash: string;
  questName: string;
  character: string;
  text: string;
  soundsBranch: string;
  databaseBranch: string;
}

export interface UploadAudioResult {
  hash: string;
  uri: string;
}

export interface LineResult {
  hash: string;
  character: string;
  status: "completed" | "skipped" | "failed";
  error?: string;
}

export interface QuestResult {
  questName: string;
  totalLines: number;
  completed: number;
  skipped: number;
  failed: number;
  results: LineResult[];
}

export interface CheckExistsInput {
  hash: string;
  soundsBranch: string;
}

export interface DatabaseInsertInput {
  quest: string;
  character: string;
  text: string;
  uri: string;
}

export interface GitHubCreateFileInput {
  path: string;
  content: Buffer;
  branch: string;
  message: string;
}

export interface GitHubUpdateFileInput {
  path: string;
  content: Buffer;
  branch: string;
  message: string;
  sha: string;
}

export interface RateLimitConfig {
  tokens: number;
  refillRate: number;
  intervalMs: number;
}

export interface CleanupVoicesInput {
  dryRun?: boolean;
}

export interface CleanupVoicesResult {
  neededCharacters: string[];
  unusedVoices: VoiceInfo[];
  deletedVoices: string[];
  failedDeletions: { name: string; error: string }[];
  dryRun: boolean;
}
