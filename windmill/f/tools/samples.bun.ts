import type { GitHubClient } from "./git";
import type { VoiceSample } from "./voice";

// 96 kbps CBR mp3 — used to estimate clip duration from file size.
const BYTES_PER_SECOND = 12000;

// Collect up to ~targetSeconds of a character's clips for IVC. IVC's sweet spot is ~1-2
// minutes; beyond ~3 it can degrade the clone, and file count is irrelevant — only total
// duration matters. Pass uris longest-first so the target is reached with fewer downloads.
export async function collectVoiceSamples(
  github: GitHubClient,
  uris: string[],
  branch: string,
  targetSeconds = 120
): Promise<VoiceSample[]> {
  const samples: VoiceSample[] = [];
  let seconds = 0;
  for (const uri of uris) {
    if (seconds >= targetSeconds) break;
    const found = await github.getFile(uri, branch);
    if (!found) continue;
    samples.push({ filename: uri, data: found.content });
    seconds += found.content.length / BYTES_PER_SECOND;
  }
  return samples;
}
