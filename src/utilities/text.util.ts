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
