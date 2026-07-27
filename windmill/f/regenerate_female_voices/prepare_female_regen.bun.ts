import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";
import type { GenerationTarget } from "../tools/types";

export interface FemaleRegenTarget extends GenerationTarget {
  // Whether a Player Female row already exists in the database for this text.
  // Regenerated-in-place lines keep the same filename (hash), so only the ones
  // NOT already in the database need a new row written at the end.
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

// Builds the work list from the player dialog rows — the authoritative set of player
// lines (sourceCharacter, "Player Male" in the plugin's v2 database, which has all 11,997
// player lines). Each unique player text maps to a Player Female target. forceRegenerate=true
// regenerates every line; false processes only the lines missing a Player Female row (the
// ones the professional clone failed to produce under v3). Also creates the feature branch
// off the sounds branch.
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
    // The hash (and therefore the audio file) is character|text, quest-independent,
    // so the same line reused across quests is one target.
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
