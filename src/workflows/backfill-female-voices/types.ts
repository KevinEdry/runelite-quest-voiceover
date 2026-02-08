export interface BackfillFemaleVoicesInput {
  readonly playerFemaleVoiceId?: string;
  readonly limit?: number;
  readonly forceRegenerate?: boolean;
}

export interface BackfillLineResult {
  readonly quest: string;
  readonly text: string;
  readonly hash: string;
  readonly status: "completed" | "skipped" | "failed";
  readonly error?: string;
}

export interface BackfillFemaleVoicesResult {
  readonly totalMaleLines: number;
  readonly missingFemaleLines: number;
  readonly completed: number;
  readonly skipped: number;
  readonly failed: number;
  readonly results: BackfillLineResult[];
  readonly featureBranch: string;
}
