import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { slugifyQuest } from "../tools/text";
import type { LineResult } from "../tools/types";

export interface QuestSummary {
  questName: string;
  totalTargets: number;
  completed: number;
  skipped: number;
  failed: number;
  rows: number;
  databaseFeatureBranch: string | null;
}

interface DialogRow {
  quest: string;
  character: string;
  text: string;
  uri: string;
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
  const completed = results.filter((r) => r?.status === "completed").length;
  const skipped = results.filter((r) => r?.status === "skipped").length;
  const failed = results.filter((r) => r?.status === "failed").length;

  // Every clip that exists (generated now or skipped because already present) is a row;
  // failed clips have no audio so they are left out. Skipped clips carry their uri, so a
  // resume still writes the quest's complete row set, not just this run's new clips.
  const byKey = new Map<string, DialogRow>();
  for (const result of results) {
    if (!result || result.status === "failed" || !result.uri) continue;
    const row: DialogRow = { quest: questName, character: result.character, text: result.text, uri: result.uri };
    byKey.set(JSON.stringify([row.character, row.text, row.uri]), row);
  }
  const rows = [...byKey.values()].sort(
    (a, b) => a.character.localeCompare(b.character) || a.text.localeCompare(b.text) || a.uri.localeCompare(b.uri)
  );

  const databaseFeatureBranch = `${featureBranch}-db`;
  const shardPath = `db/rows/${slugifyQuest(questName)}.jsonl`;
  const summary = { questName, totalTargets: results.length, completed, skipped, failed, rows: rows.length };

  if (dryRun) {
    console.log(`[DRY RUN] Would write ${rows.length} rows to ${shardPath} on ${databaseFeatureBranch}`);
    return { ...summary, databaseFeatureBranch };
  }

  if (rows.length === 0) return { ...summary, databaseFeatureBranch: null };

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  if (!(await github.branchExists(databaseFeatureBranch))) {
    await github.createBranch(databaseFeatureBranch, databaseBranch);
  }

  // A whole-quest shard under a distinct filename: parallel quests never collide (unlike the
  // old single binary .db), and re-running a quest overwrites only its own shard. The .db the
  // plugin downloads is rebuilt from all shards by the database branch's rebuild workflow.
  const content = rows.map((row) => JSON.stringify(row)).join("\n") + "\n";
  await github.createOrUpdateFile(
    shardPath,
    Buffer.from(content, "utf-8"),
    databaseFeatureBranch,
    `feat: Add ${rows.length} dialog rows for ${questName}`
  );

  return { ...summary, databaseFeatureBranch };
}
