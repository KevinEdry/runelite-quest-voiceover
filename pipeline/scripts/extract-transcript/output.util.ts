import { writeFile, mkdir } from "fs/promises";
import { join } from "path";
import type { DialogueLine, TranscriptOutput } from "./types";

export function createSlug(questName: string): string {
  return questName
    .toLowerCase()
    .replace(/[''']/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function generateBranchName(questName: string): string {
  return `quest/${createSlug(questName)}`;
}

export function buildOutputJson(
  questName: string,
  branch: string,
  lines: DialogueLine[],
  characters: string[]
): TranscriptOutput {
  return {
    branch,
    quest_name: questName,
    lines,
    characters: characters.map((name) => ({ name })),
  };
}

export async function saveJsonToFile(
  output: TranscriptOutput,
  outputDirectory: string,
  questName: string
): Promise<string> {
  await mkdir(outputDirectory, { recursive: true });

  const fileSlug = createSlug(questName);
  const outputPath = join(outputDirectory, `${fileSlug}.json`);

  await writeFile(outputPath, JSON.stringify(output, null, 2) + "\n");

  return outputPath;
}

export function printExtractionSummary(
  questName: string,
  branch: string,
  lines: DialogueLine[],
  characters: string[]
): void {
  console.error("\n=== Summary ===");
  console.error(`Quest: ${questName}`);
  console.error(`Branch: ${branch}`);
  console.error(`Total lines: ${lines.length}`);
  console.error(`Total characters: ${characters.length}`);
}
