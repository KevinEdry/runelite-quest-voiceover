export interface DialogLine {
  character: string;
  line: string;
}

export interface GenerationTarget {
  questName: string;
  character: string;
  voiceId: string;
  text: string;
  previousText?: string;
  nextText?: string;
}

export interface LineResult {
  character: string;
  text: string;
  hash: string;
  uri: string;
  status: "completed" | "skipped" | "failed";
  error?: string;
}
