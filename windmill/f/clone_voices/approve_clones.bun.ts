import * as wmill from "windmill-client";
import type { PlanVoiceClonesResult } from "./plan_voice_clones";

export async function main(plan: PlanVoiceClonesResult) {
  const urls = await wmill.getResumeUrls("approver");

  const toClone =
    plan.targets.map((t) => `- ${t.character} (${t.uris.length} candidate clips)`).join("\n") || "_none_";
  const skipped =
    plan.skipped.map((s) => `- ${s.character} — ${s.reason}`).join("\n") || "_none_";

  return {
    resume: urls.resume,
    cancel: urls.cancel,
    default_args: {},
    description: {
      render_all: [
        { markdown: `# Instant-voice-clone ${plan.targets.length} character(s)?` },
        { markdown: `## Will clone (from existing audio)\n${toClone}` },
        { markdown: `## Skipped\n${skipped}` },
      ],
    },
  };
}
