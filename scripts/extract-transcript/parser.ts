import { load } from "cheerio";
import type { DialogueLine } from "./types";

const STAGE_DIRECTION_PATTERNS = [
  /^\(.*\)$/,
  /^End of dialogue$/i,
  /^Select an Option$/i,
  /^Shows previous options$/i,
  /^Start the .* quest\?$/i,
  /^Quest complete!$/i,
  /^Congratulations!/i,
];

const DYNAMIC_TEXT_PATTERN = /\[.*?\]/g;

export function extractQuestNameFromTitle(html: string): string | null {
  const document = load(html);
  const title = document("title").text();
  const match = title.match(/Transcript of (.+?)\s*-\s*OSRS Wiki/);
  return match ? match[1].trim() : null;
}

export function extractQuestNameFromUrl(url: string): string {
  const parsedUrl = new URL(url);
  const path = parsedUrl.pathname;

  if (!path.includes("Transcript:")) {
    return "Unknown Quest";
  }

  const questPart = path.split("Transcript:").pop() || "";
  return decodeURIComponent(questPart).replace(/_/g, " ");
}

export function extractQuestName(html: string, url: string): string {
  return extractQuestNameFromTitle(html) || extractQuestNameFromUrl(url);
}

function resolveDynamicText(text: string): string {
  return text
    .replace(DYNAMIC_TEXT_PATTERN, "")
    .replace(/\s+/g, " ")
    .replace(/\s+,/g, ",")
    .replace(/,\s*,/g, ",")
    .replace(/\s+\./g, ".")
    .trim();
}

function isStageDirection(text: string): boolean {
  const trimmed = text.trim();

  if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
    return true;
  }

  return STAGE_DIRECTION_PATTERNS.some((pattern) => pattern.test(trimmed));
}

function isValidSpeakerTag(boldText: string): boolean {
  return boldText.trim().endsWith(":");
}

function extractCharacterName(speakerText: string): string {
  const name = speakerText.slice(0, -1).trim();
  if (name.toLowerCase() === "[player name]") {
    return "Player";
  }
  if (name.startsWith("[") && name.endsWith("]")) {
    return name.slice(1, -1);
  }
  return name;
}

function isMenuOptionElement(document: any, listItem: any): boolean {
  return (
    document(listItem).find(".transcript-opt").length > 0 ||
    document(listItem).closest(".transcript-opt").length > 0
  );
}

function isOverheadChatMessage(document: any, listItem: any): boolean {
  return document(listItem).find(".in-game-message").length > 0;
}

function extractDialogueTextFromSiblings(document: any, boldTag: any): string {
  const dialogueParts: string[] = [];
  let sibling = boldTag[0].nextSibling;

  while (sibling) {
    if (sibling.type === "text") {
      const text = document(sibling).text().trim();
      if (text) {
        dialogueParts.push(text);
      }
    } else if (sibling.name === "sup") {
      // [sic] annotations should not be included in voice lines
    } else if (sibling.name === "a" || sibling.name === "span") {
      const text = document(sibling).text().trim();
      if (text) {
        dialogueParts.push(text);
      }
    }
    sibling = sibling.nextSibling;
  }

  return dialogueParts.join(" ").trim();
}

export function extractDialogueLines(html: string): DialogueLine[] {
  const document = load(html);
  const content = document(".mw-parser-output");

  if (content.length === 0) {
    throw new Error("Could not find main content area");
  }

  const lines: DialogueLine[] = [];
  const seenLines = new Set<string>();

  content.find("li").each((_, listItem) => {
    if (isMenuOptionElement(document, listItem)) {
      return;
    }

    if (isOverheadChatMessage(document, listItem)) {
      return;
    }

    const boldTag = document(listItem).find("b").first();
    if (boldTag.length === 0) {
      return;
    }

    const speakerText = boldTag.text().trim();
    if (!isValidSpeakerTag(speakerText)) {
      return;
    }

    const character = extractCharacterName(speakerText);
    if (!character) {
      return;
    }

    const rawDialogue = extractDialogueTextFromSiblings(document, boldTag);
    if (!rawDialogue) {
      return;
    }

    if (isStageDirection(rawDialogue)) {
      return;
    }

    const dialogue = resolveDynamicText(rawDialogue);
    if (!dialogue) {
      return;
    }

    if (/^\.{2,}$/.test(dialogue)) {
      return;
    }

    const deduplicationKey = `${character}|${dialogue}`;
    if (seenLines.has(deduplicationKey)) {
      return;
    }
    seenLines.add(deduplicationKey);

    lines.push({
      character,
      line: dialogue,
    });
  });

  return lines;
}

export function extractUniqueCharacters(lines: DialogueLine[]): string[] {
  const characters = new Set<string>();
  for (const line of lines) {
    if (line.character.toLowerCase() !== "player") {
      characters.add(line.character);
    }
  }
  return Array.from(characters).sort();
}
