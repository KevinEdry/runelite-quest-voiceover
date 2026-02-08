export interface DialogueLine {
  character: string;
  line: string;
}

export interface Character {
  name: string;
}

export interface TranscriptOutput {
  branch: string;
  quest_name: string;
  lines: DialogueLine[];
  characters: Character[];
}
