import * as wmill from "windmill-client";
import { createGitHubClient } from "../tools/git";
import { openDialogsDatabase } from "../tools/database";

export interface QuestOption {
  value: string;
  label: string;
}

// Transcript bodies are read from raw.githubusercontent.com in parallel batches: this backs
// a dropdown, so ~200 sequential fetches would be too slow, and raw reads (unlike the
// Contents API) don't draw down the REST rate limit.
const READ_CONCURRENCY = 30;

export async function main(
  githubOwner = "KevinEdry",
  githubRepo = "runelite-quest-voiceover",
  transcriptsBranch = "automations",
  transcriptsPath = "transcripts",
  databaseBranch = "database"
): Promise<QuestOption[]> {
  const githubToken = await wmill.getVariable("f/quest_voiceover/github_token");
  const github = createGitHubClient({ token: githubToken, owner: githubOwner, repo: githubRepo });

  const dialogs = await openDialogsDatabase(github, databaseBranch, { readonly: true });
  const voiced = new Set(dialogs.getQuests());
  dialogs.database.close();
  dialogs.cleanup();

  const files = [...(await github.listBranchFiles(transcriptsBranch))].filter(
    (file) => file.startsWith(`${transcriptsPath}/`) && file.endsWith(".json")
  );

  const readQuest = async (file: string) => {
    const content = await github.getRawFile(file, transcriptsBranch);
    if (!content) return null;
    const questName = (JSON.parse(content.toString("utf-8")) as { quest_name: string }).quest_name;
    return { value: file, questName, generated: voiced.has(questName) };
  };

  const quests: { value: string; questName: string; generated: boolean }[] = [];
  for (let index = 0; index < files.length; index += READ_CONCURRENCY) {
    const batch = await Promise.all(files.slice(index, index + READ_CONCURRENCY).map(readQuest));
    for (const quest of batch) {
      if (quest) quests.push(quest);
    }
  }

  quests.sort((a, b) => Number(a.generated) - Number(b.generated) || a.questName.localeCompare(b.questName));

  return quests.map((quest) => ({
    value: quest.value,
    label: `${quest.generated ? "[x]" : "[ ]"} ${quest.questName}`,
  }));
}
