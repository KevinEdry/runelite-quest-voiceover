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
  const breakdown = plan.characters.map((c) => `- ${c.character} — ${label[c.status]}`).join("\n");
  const list = (names: string[], empty: string) =>
    names.length > 0 ? names.map((name) => `- ${name}`).join("\n") : empty;

  const quota = plan.subscription
    ? `**Plan:** ${plan.subscription.tier} · ${plan.subscription.remaining.toLocaleString()} of ` +
      `${plan.subscription.characterLimit.toLocaleString()} characters remaining this cycle`
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
            `**Clips to generate:** ${plan.clipsToGenerate.toLocaleString()} ` +
            `(of ${plan.totalClips.toLocaleString()} total; ${plan.alreadyGenerated.toLocaleString()} already done, skipped)\n\n` +
            `**Estimated characters:** ${plan.estimatedCharacters.toLocaleString()}\n\n` +
            `**Estimated cost:** $${plan.estimatedCostUsd} (at $${plan.costPer1kCharacters} / 1K characters)\n\n` +
            quota,
        },
        { markdown: `## Voices to clone from existing audio (${plan.voicesToClone.length})\n${list(plan.voicesToClone, "_none_")}` },
        { markdown: `## New voices to create (${plan.voicesToCreate.length})\n${list(plan.voicesToCreate, "_none_")}` },
        { markdown: `## Character → voice\n${breakdown}` },
      ],
    },
  };
}
