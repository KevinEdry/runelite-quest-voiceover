import { createHash } from "crypto";

// Template tokens like [player name] / [he/she] would otherwise be read aloud verbatim.
export function removeSpecialCharacters(line: string): string {
  return line
    .replace(/\[player name\]/gi, "")
    .replace(/\[1-19\]/g, "")
    .replace(/\[boy\/girl\]/gi, "boy")
    .replace(/\[#\]/g, "")
    .replace(/\[ball\/balls\]/gi, "ball")
    .replace(/\[lad\/lass\]/gi, "lad")
    .replace(/\[he\/she\]/gi, "he")
    .replace(/\[his\/her\]/gi, "his")
    .replace(/\[him\/her\]/gi, "him")
    .replace(/\[man\/woman\]/gi, "man")
    .replace(/\[sir\/madam\]/gi, "sir")
    .replace(/\[brother\/sister\]/gi, "brother")
    .replace(/\[son\/daughter\]/gi, "son")
    .trim();
}

// This hash is the audio filename convention shared with the sounds branch and the plugin.
export function generateDialogHash(character: string, line: string): string {
  return createHash("md5").update(`${character}|${line}`).digest("hex");
}
