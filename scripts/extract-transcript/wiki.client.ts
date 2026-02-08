const USER_AGENT =
  "OSRS-Quest-Voiceover-Extractor/1.0 (https://github.com/runelite-quest-voiceover)";

export async function fetchWikiPage(url: string): Promise<string> {
  const response = await fetch(url, {
    headers: { "User-Agent": USER_AGENT },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch wiki page: ${response.statusText}`);
  }

  return response.text();
}
