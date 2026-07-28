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
  databaseFeatureBranch: string | null;
}

export async function main(
  questName: string,
  results: LineResult[],
  githubOwner: string,
  githubRepo: string,
  featureBranch: string,
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
  const databaseFeatureBranch = `${featureBranch}-db`;

  const summary = { questName, totalTargets: results.length, completed, skipped, failed, inserted };

  if (dryRun) {
    console.log(`[DRY RUN] Would write ${inserted} rows to ${databaseFeatureBranch}`);
    dialogs.database.close();
    dialogs.cleanup();
    return { ...summary, databaseFeatureBranch };
  }

  if (inserted === 0) {
    dialogs.database.close();
    dialogs.cleanup();
    return { ...summary, databaseFeatureBranch: null };
  }

  if (!(await github.branchExists(databaseFeatureBranch))) {
    await github.createBranch(databaseFeatureBranch, databaseBranch);
  }
  await dialogs.save(databaseFeatureBranch, `feat: Add ${inserted} dialog rows for ${questName}`);

  return { ...summary, databaseFeatureBranch };
}
