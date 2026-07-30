import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";

export interface OpenedPullRequests {
  soundsPullRequest: string | null;
  databasePullRequest: string | null;
}

export async function main(
  questName: string,
  featureBranch: string,
  databaseFeatureBranch: string | null,
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  databaseBranch = "database",
  dryRun = false
): Promise<OpenedPullRequests> {
  if (dryRun) {
    console.log(
      `[DRY RUN] Would open PRs: ${featureBranch} -> ${soundsBranch}` +
        (databaseFeatureBranch ? ` and ${databaseFeatureBranch} -> ${databaseBranch}` : "")
    );
    return { soundsPullRequest: null, databasePullRequest: null };
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const audio = await github.createPullRequest({
    head: featureBranch,
    base: soundsBranch,
    title: `feat(sounds): ${questName} voice lines`,
    body: `Generated voice-line audio for **${questName}**, produced by the quest voiceover pipeline.`,
  });

  // Only when write_database wrote a shard branch (a dry run or a quest with no new rows
  // leaves databaseFeatureBranch null).
  const database = databaseFeatureBranch
    ? await github.createPullRequest({
        head: databaseFeatureBranch,
        base: databaseBranch,
        title: `feat(db): ${questName} dialog rows`,
        body: `Dialog row shard for **${questName}**. The database branch's workflow rebuilds quest_voiceover_v2.db from all shards on merge.`,
      })
    : null;

  console.log(`sounds PR: ${audio?.url ?? "none"} | database PR: ${database?.url ?? "none"}`);
  return { soundsPullRequest: audio?.url ?? null, databasePullRequest: database?.url ?? null };
}
