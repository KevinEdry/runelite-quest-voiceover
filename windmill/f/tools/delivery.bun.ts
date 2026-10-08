import { withRetry } from "./retry";
import type { DialogLine } from "./types";

const ANNOTATION_MODEL = "claude-sonnet-5";
const BATCH_SIZE = 50;
const CONTEXT_LINES = 4;

export interface DeliveryAnnotator {
  readonly annotate: (
    lines: readonly DialogLine[],
    descriptions: Record<string, string>
  ) => Promise<string[]>;
}

const SYSTEM_PROMPT = [
  "You are a voice director adding ElevenLabs v4 audio tags to quest dialogue so a text-to-speech model delivers each line with the right emotion and pacing.",
  "Audio tags are square-bracketed delivery cues placed inline in the text, e.g. [annoyed], [whispering], [shouting], [nervous, urgent], [dry amusement], [quietly, resigned], [Pause], [sighs].",
  "Rules:",
  "- Preserve every original word and all punctuation exactly. Only insert bracketed tags; never rewrite, translate, or remove words.",
  "- Tag for the delivery the line's content and the speaker's personality imply. A leading tag sets the whole line's tone; an inline [Pause] or mid-line tag shifts it.",
  "- Be tasteful: many lines need no tag at all. Leave flat or purely functional lines untagged. Never tag every line by reflex.",
  "- Use at most two tags per line.",
  "- Return ONLY a JSON array of strings, one per input line, in the same order. Each string is that line's text with any tags added, or the unchanged text if no tag fits.",
].join("\n");

function parseTaggedArray(responseText: string, expected: number): string[] | null {
  const start = responseText.indexOf("[");
  const end = responseText.lastIndexOf("]");
  if (start < 0 || end <= start) return null;
  try {
    const parsed = JSON.parse(responseText.slice(start, end + 1));
    if (!Array.isArray(parsed) || parsed.length !== expected) return null;
    if (!parsed.every((entry) => typeof entry === "string")) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function createDeliveryAnnotator(apiKey: string, model: string = ANNOTATION_MODEL): DeliveryAnnotator {
  const annotateBatch = async (
    batch: readonly DialogLine[],
    context: readonly DialogLine[],
    descriptions: Record<string, string>
  ): Promise<string[]> => {
    const cast = [...new Set(batch.map((line) => line.character))]
      .map((name) => `${name}: ${descriptions[name] ?? "no description"}`)
      .join("\n");
    const contextBlock = context.length
      ? `Preceding lines (context only, do not return these):\n${context.map((l) => `${l.character}: ${l.line}`).join("\n")}\n\n`
      : "";
    const numbered = batch.map((l, i) => `${i}. ${l.character}: ${l.line}`).join("\n");
    const userPrompt =
      `Characters:\n${cast}\n\n${contextBlock}Lines to tag (return a JSON array of ${batch.length} strings, index-aligned):\n${numbered}`;

    const response = await withRetry(async () => {
      const result = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "x-api-key": apiKey,
          "anthropic-version": "2023-06-01",
          "content-type": "application/json",
        },
        body: JSON.stringify({
          model,
          max_tokens: 8192,
          system: SYSTEM_PROMPT,
          messages: [{ role: "user", content: userPrompt }],
        }),
      });
      if (!result.ok) throw new Error(`Anthropic API ${result.status}: ${await result.text()}`);
      return result;
    });

    const data: { content: { type: string; text: string }[] } = await response.json();
    const text = data.content.find((part) => part.type === "text")?.text ?? "";
    const tagged = parseTaggedArray(text, batch.length);
    return tagged ?? batch.map((line) => line.line);
  };

  const annotate = async (
    lines: readonly DialogLine[],
    descriptions: Record<string, string>
  ): Promise<string[]> => {
    const out: string[] = [];
    for (let start = 0; start < lines.length; start += BATCH_SIZE) {
      const batch = lines.slice(start, start + BATCH_SIZE);
      const context = lines.slice(Math.max(0, start - CONTEXT_LINES), start);
      try {
        out.push(...(await annotateBatch(batch, context, descriptions)));
      } catch (error) {
        console.error(`Delivery annotation failed for a batch, using plain text: ${error instanceof Error ? error.message : String(error)}`);
        out.push(...batch.map((line) => line.line));
      }
    }
    return out;
  };

  return { annotate };
}
