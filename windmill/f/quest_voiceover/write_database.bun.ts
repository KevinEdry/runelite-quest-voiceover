import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import type { LineResult } from "../tools/types";

export interface QuestSummary {
  questName: string;
  totalTargets: number;
  completed: number;
  skipped: number;
  failed: number;
  inserted: number;
}

// Consolidated database write. Every generation job is independent, so the
// download -> insert-all -> upload cycle happens exactly once, at the end.
export async function main(
  questName: string,
  results: LineResult[],
  githubOwner: string,
  githubRepo: string,
  databaseBranch = "database",
  dryRun = false
): Promise<QuestSummary> {
  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const dialogs = await openDialogsDatabase(github, databaseBranch);

  let inserted = 0;
  for (const result of results) {
    if (!result || result.status === "failed" || !result.uri) continue;
    if (dialogs.insert({ quest: questName, character: result.character, text: result.text, uri: result.uri })) {
      inserted++;
    }
  }

  const completed = results.filter((r) => r?.status === "completed").length;
  const skipped = results.filter((r) => r?.status === "skipped").length;
  const failed = results.filter((r) => r?.status === "failed").length;

  if (dryRun) {
    console.log(`[DRY RUN] Would upload database with ${inserted} new rows`);
    dialogs.database.close();
    dialogs.cleanup();
  } else if (inserted > 0) {
    await dialogs.save(databaseBranch, "feat: Update quest voiceover database");
  } else {
    console.log("No new rows; skipping database upload");
    dialogs.database.close();
    dialogs.cleanup();
  }

  return { questName, totalTargets: results.length, completed, skipped, failed, inserted };
}
