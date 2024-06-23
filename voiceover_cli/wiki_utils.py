import requests
from bs4 import BeautifulSoup
from typing import TypedDict, List

QUESTS_TRANSCRIPT_WIKI_URL = "https://oldschool.runescape.wiki/w/Category:Quest_transcript"
QUEST_TRANSCRIPT_WIKI_BASE_URL = "https://oldschool.runescape.wiki"

class QuestTranscriptMetadata(TypedDict):
    idx: int
    title: str
    link: str


class QuestTranscript(TypedDict):
    transcript: dict[str, list[str]]
    flattened_transcript: list[(str, str)]


def get_quests() -> List[QuestTranscriptMetadata]:
    response = requests.get(QUESTS_TRANSCRIPT_WIKI_URL)
    soup = BeautifulSoup(response.content, 'html.parser')
    
    quest_transcripts_list: List[QuestTranscriptMetadata] = []

    for i, li in enumerate(soup.select('div.mw-category-group li')):
        if li.name == 'li' and li.find('a', recursive=False):
            a = li.find('a', recursive=False)
            quest_transcripts_list.append({'idx': i, 
                                            'title': a.get_text(strip=True, separator=' '), 
                                            'link': f"{QUEST_TRANSCRIPT_WIKI_BASE_URL}{a.attrs['href']}"})
        
    return quest_transcripts_list


def get_transcript(url) -> QuestTranscript:
    response = requests.get(url)
    soup = BeautifulSoup(response.content, 'html.parser')

    transcript_list = soup.find('div', class_='mw-parser-output')
    if not transcript_list:
        raise Exception("Dialog list not found")

    transcript: dict[str, list[str]] = {}
    flatten_transcript: list[(str, str)] = []
    character = None

    for elem in transcript_list.find_all('li'):
        if elem.name == 'li' and elem.find('b', recursive=False):
            character = elem.find('b', recursive=False).extract().text.strip().replace(":", "")

            if character not in transcript:
                transcript[character] = []

            line = elem.get_text(strip=True, separator=' ')
            transcript[character].append(line)
            flatten_transcript.append((character, line))

    return {'transcript': transcript, 'flattened_transcript': flatten_transcript}