import * as wmill from "windmill-client";
import type { QuestPlan } from "./plan_generation";

export async function main(plan: QuestPlan) {
  const urls = await wmill.getResumeUrls("approver");

  const label: Record<string, string> = {
    matched: "existing voice",
    to_clone: "clone from existing audio",
    to_create: "new voice",
    missing: "NO VOICE (skipped)",
  };
  const breakdown = plan.characters
    .map((c) => `| ${c.character} | ${c.status === "matched" ? c.voiceId : label[c.status]} |`)
    .join("\n");

  const list = (names: string[], empty: string) =>
    names.length > 0 ? names.map((name) => `- ${name}`).join("\n") : empty;
  const voicesToClone = list(plan.voicesToClone, "_none_");
  const voicesToCreate = list(plan.voicesToCreate, "_none_");

  const quota = plan.subscription
    ? `**Plan:** ${plan.subscription.tier} · used ${plan.subscription.characterCount.toLocaleString()} / ` +
      `${plan.subscription.characterLimit.toLocaleString()} · **${plan.subscription.remaining.toLocaleString()} characters remaining** this cycle`
    : "_subscription/quota unavailable_";

  return {
    resume: urls.resume,
    cancel: urls.cancel,
    default_args: {},
    description: {
      render_all: [
        { markdown: `# Generate voiceover — ${plan.questName}` },
        {
          markdown:
            `**Lines:** ${plan.totalLines} · **Clips to generate:** ${plan.totalClips}\n\n` +
            `**Estimated characters:** ${plan.estimatedCharacters.toLocaleString()}\n\n` +
            `**Estimated cost:** $${plan.estimatedCostUsd} (at $${plan.costPer1kCharacters} / 1K characters)\n\n` +
            quota,
        },
        { markdown: `## Voices to clone from existing audio (${plan.voicesToClone.length})\n${voicesToClone}` },
        { markdown: `## New voices to create (${plan.voicesToCreate.length})\n${voicesToCreate}` },
        { markdown: `## Character → voice\n| Character | Voice |\n|---|---|\n${breakdown}` },
      ],
    },
  };
}
