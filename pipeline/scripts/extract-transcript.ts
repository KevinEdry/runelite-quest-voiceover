#!/usr/bin/env bun
/**
 * OSRS Wiki Quest Transcript Extractor
 *
 * Extracts dialogue lines from OSRS Wiki quest transcript pages and outputs JSON.
 *
 * Usage:
 *   bun run scripts/extract-transcript.ts <wiki_url>
 *   bun run scripts/extract-transcript.ts https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid
 *
 * Output:
 *   JSON file saved to transcripts/{quest-name-slug}.json
 */

import { fetchWikiPage } from "./extract-transcript/wiki.client";
import {
  extractQuestName,
  extractDialogueLines,
  extractUniqueCharacters,
} from "./extract-transcript/parser";
import {
  generateBranchName,
  buildOutputJson,
  saveJsonToFile,
  printExtractionSummary,
} from "./extract-transcript/output.util";

function parseArguments(commandLineArguments: string[]): {
  url: string;
  outputDirectory: string;
  useStdout: boolean;
} {
  const cliArguments = commandLineArguments.slice(2);

  if (cliArguments.length === 0 || cliArguments.includes("--help") || cliArguments.includes("-h")) {
    console.error("Usage: bun run scripts/extract-transcript.ts <wiki_url>");
    console.error(
      "Example: bun run scripts/extract-transcript.ts https://oldschool.runescape.wiki/w/Transcript:Cooks_Assistant"
    );
    console.error("\nOptions:");
    console.error(
      "  -o, --output <dir>   Output directory (default: transcripts/)"
    );
    console.error(
      "  --stdout            Print JSON to stdout instead of saving to file"
    );
    process.exit(cliArguments.includes("--help") || cliArguments.includes("-h") ? 0 : 1);
  }

  let url = "";
  let outputDirectory = "transcripts";
  let useStdout = false;

  const iterator = cliArguments[Symbol.iterator]();
  for (const argument of iterator) {
    if (argument === "-o" || argument === "--output") {
      outputDirectory = iterator.next().value;
    } else if (argument === "--stdout") {
      useStdout = true;
    } else if (!url) {
      url = argument;
    }
  }

  if (!url) {
    console.error("Error: Wiki URL is required");
    process.exit(1);
  }

  return { url, outputDirectory, useStdout };
}

async function main() {
  const { url, outputDirectory, useStdout } = parseArguments(process.argv);

  console.error(`Fetching: ${url}`);
  const html = await fetchWikiPage(url);

  const questName = extractQuestName(html, url);
  console.error(`Quest: ${questName}`);

  const branch = generateBranchName(questName);
  console.error(`Branch: ${branch}`);

  const lines = extractDialogueLines(html);
  console.error(`Lines extracted: ${lines.length}`);

  const characters = extractUniqueCharacters(lines);
  console.error(`Characters: ${characters.length}`);
  for (const character of characters) {
    console.error(`  - ${character}`);
  }

  const output = buildOutputJson(questName, branch, lines, characters);

  if (useStdout) {
    console.log(JSON.stringify(output, null, 2));
    return;
  }

  const outputPath = await saveJsonToFile(output, outputDirectory, questName);
  console.error(`Output saved to: ${outputPath}`);
  printExtractionSummary(questName, branch, lines, characters);
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
