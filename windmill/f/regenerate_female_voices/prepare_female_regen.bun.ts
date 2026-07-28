import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import type { GenerationTarget } from "../tools/types";

export interface FemaleRegenTarget extends GenerationTarget {
  // Regenerating overwrites the same filename, so only lines without an existing row
  // need one written at the end.
  alreadyInDatabase: boolean;
}

export interface PrepareFemaleRegenResult {
  featureBranch: string;
  soundsBranch: string;
  databaseBranch: string;
  totalPlayerMaleLines: number;
  missingFemaleLines: number;
  targets: FemaleRegenTarget[];
}

// sourceCharacter ("Player Male") is the authoritative set of player lines — the plugin's
// v2 database has every player line under it.
export async function main(
  githubOwner: string,
  githubRepo: string,
  soundsBranch = "sounds",
  databaseBranch = "database",
  featureBranch?: string,
  forceRegenerate = true,
  limit?: number,
  playerFemaleVoiceId?: string,
  sourceCharacter = "Player Male"
): Promise<PrepareFemaleRegenResult> {
  const voiceId =
    playerFemaleVoiceId || (await wmill.getVariable("f/quest_voiceover/player_female_voice_id"));
  if (!voiceId) {
    throw new Error(
      "Player Female voice ID required: pass playerFemaleVoiceId or set the f/quest_voiceover/player_female_voice_id variable"
    );
  }

  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });

  const seenTexts = new Set<string>();
  const allTargets: FemaleRegenTarget[] = [];
  for (const line of dialogs.getByCharacter(sourceCharacter)) {
    // The hash is quest-independent, so a line reused across quests is a single target.
    if (seenTexts.has(line.text)) continue;
    seenTexts.add(line.text);
    allTargets.push({
      questName: line.quest,
      character: "Player Female",
      voiceId,
      text: line.text,
      alreadyInDatabase: dialogs.exists("Player Female", line.text),
    });
  }
  dialogs.database.close();
  dialogs.cleanup();

  const missingFemaleLines = allTargets.filter((target) => !target.alreadyInDatabase).length;

  const selected = forceRegenerate
    ? allTargets
    : allTargets.filter((target) => !target.alreadyInDatabase);
  const targets = typeof limit === "number" ? selected.slice(0, limit) : selected;

  console.log(
    `${seenTexts.size} unique "${sourceCharacter}" lines, ${missingFemaleLines} missing Player Female. ` +
      `${forceRegenerate ? "Regenerating all" : "Generating missing only"} — ${targets.length} targets` +
      (typeof limit === "number" ? ` (limited to ${limit})` : "")
  );

  const now = new Date().toISOString();
  const timestamp = `${now.slice(0, 10).replace(/-/g, "")}-${now.slice(11, 19).replace(/:/g, "")}`;
  const branch = featureBranch || `regen-female-voices-${timestamp}`;
  if (await github.branchExists(branch)) {
    console.log(`Feature branch ${branch} already exists, reusing it`);
  } else {
    await github.createBranch(branch, soundsBranch);
  }

  return {
    featureBranch: branch,
    soundsBranch,
    databaseBranch,
    totalPlayerMaleLines: seenTexts.size,
    missingFemaleLines,
    targets,
  };
}
