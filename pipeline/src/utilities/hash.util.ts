import { createHash } from "crypto";

export function generateDialogHash(character: string, line: string): string {
  const input = `${character}|${line}`;
  return createHash("md5").update(input).digest("hex");
}
