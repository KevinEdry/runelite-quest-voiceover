import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import type { RegenLineResult } from "./generate_female_line";

export interface WriteFemaleDatabaseResult {
  databaseFeatureBranch: string | null;
  completed: number;
  failed: number;
  inserted: number;
}

// Adds the previously-missing Player Female rows to the database. Regenerated-in-place
// lines keep the same filename, so their existing rows already point at the right file
// and need no change — insert() dedupes them out. The new rows reference audio that
// lives on the audio feature branch, so they are written to a matching feature branch
// off the database branch; merge both branches together.
export async function main(
  results: RegenLineResult[],
  githubOwner: string,
  githubRepo: string,
  featureBranch: string,
  databaseBranch = "database",
  updateDatabase = true,
  dryRun = false
): Promise<WriteFemaleDatabaseResult> {
  const completed = results.filter((r) => r?.status === "completed").length;
  const failed = results.filter((r) => r?.status === "failed").length;

  if (!updateDatabase) {
    console.log("updateDatabase=false — skipping database write");
    return { databaseFeatureBranch: null, completed, failed, inserted: 0 };
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const dialogs = await openDialogsDatabase(github, databaseBranch);

  // Insert every line that produced a file (completed, or skipped-because-already-
  // regenerated on a resume) — not failures. insert() dedupes, so rows already present
  // from an earlier attempt are harmless.
  let inserted = 0;
  for (const result of results) {
    if (!result || result.status === "failed" || !result.uri) continue;
    if (dialogs.insert({ quest: result.quest, character: result.character, text: result.text, uri: result.uri })) {
      inserted++;
    }
  }

  const databaseFeatureBranch = `${featureBranch}-db`;

  if (dryRun) {
    console.log(`[DRY RUN] Would write ${inserted} new rows to ${databaseFeatureBranch}`);
    dialogs.database.close();
    dialogs.cleanup();
    return { databaseFeatureBranch, completed, failed, inserted };
  }

  if (inserted === 0) {
    console.log("No missing rows to add; skipping database write");
    dialogs.database.close();
    dialogs.cleanup();
    return { databaseFeatureBranch: null, completed, failed, inserted };
  }

  if (!(await github.branchExists(databaseFeatureBranch))) {
    await github.createBranch(databaseFeatureBranch, databaseBranch);
  }
  await dialogs.save(databaseFeatureBranch, `feat: Add ${inserted} regenerated Player Female dialog rows`);

  return { databaseFeatureBranch, completed, failed, inserted };
}
