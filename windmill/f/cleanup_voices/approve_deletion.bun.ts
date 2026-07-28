import * as wmill from "windmill-client";
import type { VoiceInfo } from "../tools/voice";

export async function main(unused: VoiceInfo[]) {
  const urls = await wmill.getResumeUrls("approver");
  const list = unused.map((voice) => `- ${voice.name} \`${voice.voiceId}\``).join("\n");

  return {
    resume: urls.resume,
    cancel: urls.cancel,
    default_args: {},
    description: {
      render_all: [
        { markdown: `# Delete ${unused.length} ElevenLabs voice(s)?` },
        {
          markdown: unused.length > 0 ? `## Voices to delete\n${list}` : "_No unused voices found — nothing to delete._",
        },
      ],
    },
  };
}
