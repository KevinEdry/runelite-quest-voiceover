import { createHash } from "crypto";

// Strips OSRS dialogue template tokens ([player name], [he/she], ...) that would
// otherwise be read aloud, normalising gendered variants to a single form.
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

// Audio filenames are the MD5 of `${character}|${line}`, keeping references stable
// between the database and the sounds branch.
export function generateDialogHash(character: string, line: string): string {
  return createHash("md5").update(`${character}|${line}`).digest("hex");
}
