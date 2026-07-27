import type { DialogLine, GenerationTarget, VoiceMap } from "./lib";

// Flattens the quest's dialog lines into one generation target per voice. A
// "Player" line fans out to both Player Male and Player Female; lines whose
// character has no voice in the map are dropped (logged). Neighbour text is
// attached here so the per-line job stays self-contained.
export async function main(
  questName: string,
  lines: DialogLine[],
  voiceMap: VoiceMap
): Promise<GenerationTarget[]> {
  const targets: GenerationTarget[] = [];

  for (const [index, line] of lines.entries()) {
    const previousText = lines[index - 1]?.line;
    const nextText = lines[index + 1]?.line;

    const characters =
      line.character === "Player" ? ["Player Male", "Player Female"] : [line.character];

    for (const character of characters) {
      const voiceId = voiceMap[character];
      if (!voiceId) {
        console.warn(`No voice for "${character}", skipping: "${line.line.substring(0, 50)}"`);
        continue;
      }
      targets.push({ questName, character, voiceId, text: line.line, previousText, nextText });
    }
  }

  return targets;
}
