#!/usr/bin/env python3
"""
OSRS Wiki Quest Transcript Extractor

Extracts dialogue lines from OSRS Wiki quest transcript pages and outputs JSON.

Usage:
    python extract_transcript.py <wiki_url>
    python extract_transcript.py https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid

Output:
    JSON file saved to transcripts/{quest-name-slug}.json
"""

import argparse
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlparse

import requests
from bs4 import BeautifulSoup, NavigableString

STAGE_DIRECTION_PATTERNS = [
    r"^\(.*\)$",
    r"^End of dialogue$",
    r"^Select an Option$",
    r"^Shows previous options$",
    r"^Start the .* quest\?$",
    r"^Quest complete!$",
    r"^Congratulations!",
]

DYNAMIC_TEXT_PATTERN = re.compile(r"\[.*?\]", re.IGNORECASE)


def fetch_wiki_page(url: str) -> str:
    headers = {
        "User-Agent": "OSRS-Quest-Voiceover-Extractor/1.0 (https://github.com/runelite-quest-voiceover)"
    }
    response = requests.get(url, headers=headers, timeout=30)
    response.raise_for_status()
    return response.text


def extract_quest_name_from_title(soup: BeautifulSoup) -> str | None:
    title_tag = soup.find("title")
    if not title_tag:
        return None

    title = title_tag.get_text()
    match = re.search(r"Transcript of (.+?)\s*-\s*OSRS Wiki", title)
    if match:
        return match.group(1).strip()

    return None


def extract_quest_name_from_url(url: str) -> str:
    path = urlparse(url).path
    if "Transcript:" not in path:
        return "Unknown Quest"

    quest_part = path.split("Transcript:")[-1]
    return unquote(quest_part).replace("_", " ")


def extract_quest_name(html: str, url: str) -> str:
    soup = BeautifulSoup(html, "html.parser")

    quest_name = extract_quest_name_from_title(soup)
    if quest_name:
        return quest_name

    return extract_quest_name_from_url(url)


def create_slug(quest_name: str) -> str:
    slug = quest_name.lower()
    slug = re.sub(r"[''']", "", slug)
    slug = re.sub(r"[^a-z0-9]+", "-", slug)
    return slug.strip("-")


def generate_branch_name(quest_name: str) -> str:
    return f"quest/{create_slug(quest_name)}"


def resolve_dynamic_text(text: str) -> str:
    """
    Removes wiki template variables like [Player name], [1-19], [number] that
    represent dynamic in-game content which cannot be voice acted.
    """
    result = DYNAMIC_TEXT_PATTERN.sub("", text)

    result = re.sub(r"\s+", " ", result)
    result = re.sub(r"\s+,", ",", result)
    result = re.sub(r",\s*,", ",", result)
    result = re.sub(r"\s+\.", ".", result)

    return result.strip()


def is_stage_direction(text: str) -> bool:
    text = text.strip()

    if text.startswith("(") and text.endswith(")"):
        return True

    return any(
        re.match(pattern, text, re.IGNORECASE)
        for pattern in STAGE_DIRECTION_PATTERNS
    )


def is_valid_speaker_tag(bold_text: str) -> bool:
    return bold_text.strip().endswith(":")


def extract_character_name(speaker_text: str) -> str:
    return speaker_text[:-1].strip()


def is_menu_option_element(list_item) -> bool:
    return (
        list_item.find("div", class_="transcript-opt") is not None
        or list_item.find_parent("div", class_="transcript-opt") is not None
    )


def is_overhead_chat_message(list_item) -> bool:
    """
    Overhead chat messages (yellow text above NPCs) are wrapped in
    <span class="in-game-message"> and should not be extracted as
    they don't appear in the dialogue box.
    """
    return list_item.find("span", class_="in-game-message") is not None


def extract_dialogue_text_from_siblings(bold_tag) -> str:
    dialogue_parts = []

    for sibling in bold_tag.next_siblings:
        if isinstance(sibling, NavigableString):
            text = str(sibling).strip()
            if text:
                dialogue_parts.append(text)
        elif sibling.name == "sup":
            # [sic] annotations should not be included in voice lines
            continue
        elif sibling.name in ["a", "span"]:
            text = sibling.get_text().strip()
            if text:
                dialogue_parts.append(text)

    return " ".join(dialogue_parts).strip()


def extract_dialogue_lines(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")

    content = soup.find("div", class_="mw-parser-output")
    if not content:
        raise ValueError("Could not find main content area")

    lines = []
    seen_lines = set()

    for list_item in content.find_all("li"):
        if is_menu_option_element(list_item):
            continue

        if is_overhead_chat_message(list_item):
            continue

        bold_tag = list_item.find("b")
        if not bold_tag:
            continue

        speaker_text = bold_tag.get_text().strip()
        if not is_valid_speaker_tag(speaker_text):
            continue

        character = extract_character_name(speaker_text)
        if not character:
            continue

        dialogue = extract_dialogue_text_from_siblings(bold_tag)
        if not dialogue:
            continue

        if is_stage_direction(dialogue):
            continue

        dialogue = resolve_dynamic_text(dialogue)
        if not dialogue:
            continue

        deduplication_key = (character, dialogue)
        if deduplication_key in seen_lines:
            continue
        seen_lines.add(deduplication_key)

        lines.append({
            "character": character,
            "line": dialogue
        })

    return lines


def extract_unique_characters(lines: list[dict]) -> list[str]:
    characters = set()
    for line in lines:
        character = line["character"]
        if character.lower() != "player":
            characters.add(character)
    return sorted(characters)


def build_output_json(quest_name: str, branch: str, lines: list[dict], characters: list[str]) -> dict:
    return {
        "branch": branch,
        "quest_name": quest_name,
        "lines": lines,
        "characters": [{"name": character} for character in characters]
    }


def save_json_to_file(output: dict, output_directory: Path, quest_name: str) -> Path:
    output_directory.mkdir(parents=True, exist_ok=True)

    file_slug = create_slug(quest_name)
    output_path = output_directory / f"{file_slug}.json"

    with open(output_path, "w", encoding="utf-8") as file:
        json.dump(output, indent=2, ensure_ascii=False, fp=file)

    return output_path


def print_extraction_summary(quest_name: str, branch: str, lines: list[dict], characters: list[str]):
    print(f"\n=== Summary ===", file=sys.stderr)
    print(f"Quest: {quest_name}", file=sys.stderr)
    print(f"Branch: {branch}", file=sys.stderr)
    print(f"Total lines: {len(lines)}", file=sys.stderr)
    print(f"Total characters: {len(characters)}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(
        description="Extract quest transcript from OSRS Wiki page"
    )
    parser.add_argument(
        "url",
        help="OSRS Wiki transcript URL (e.g., https://oldschool.runescape.wiki/w/Transcript:Cooks_Assistant)"
    )
    parser.add_argument(
        "-o", "--output",
        help="Output directory (default: transcripts/)",
        default="transcripts"
    )
    parser.add_argument(
        "--stdout",
        action="store_true",
        help="Print JSON to stdout instead of saving to file"
    )

    arguments = parser.parse_args()

    print(f"Fetching: {arguments.url}", file=sys.stderr)
    html = fetch_wiki_page(arguments.url)

    quest_name = extract_quest_name(html, arguments.url)
    print(f"Quest: {quest_name}", file=sys.stderr)

    branch = generate_branch_name(quest_name)
    print(f"Branch: {branch}", file=sys.stderr)

    lines = extract_dialogue_lines(html)
    print(f"Lines extracted: {len(lines)}", file=sys.stderr)

    characters = extract_unique_characters(lines)
    print(f"Characters: {len(characters)}", file=sys.stderr)
    for character in characters:
        print(f"  - {character}", file=sys.stderr)

    output = build_output_json(quest_name, branch, lines, characters)

    if arguments.stdout:
        print(json.dumps(output, indent=2, ensure_ascii=False))
        return

    output_path = save_json_to_file(output, Path(arguments.output), quest_name)
    print(f"Output saved to: {output_path}", file=sys.stderr)
    print_extraction_summary(quest_name, branch, lines, characters)


if __name__ == "__main__":
    main()
