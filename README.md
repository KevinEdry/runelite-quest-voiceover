![](https://runelite.net/img/logo.png)
# Quest Voiceover

A RuneLite plugin that adds AI-generated voice acting to Old School RuneScape quest dialogues. Experience quests the way they were meant to be heard.

<a href="https://discord.gg/tkr6tEbXJr" target="_blank">
   <img src="https://discord.com/api/guilds/1254623476086472758/widget.png?style=banner2" alt="Discord Banner 2"/>
</a>

<br>

https://github.com/KevinEdry/runelite-quest-voiceover/assets/42734162/3ecd069a-e26a-4a0f-9d7e-36af3b157a88

## Features

### Voice Acting
- **AI-Generated Voices** - Natural-sounding character voices powered by ElevenLabs
- **Automatic Playback** - Voiceovers play automatically when quest dialogs open
- **Streamed Audio** - MP3 files stream directly from GitHub, no large downloads required

### Audio Controls
- **Adjustable Volume** - Set voiceover volume independently (1-100%)
- **Audio Ducking** - Game music and sound effects automatically lower while voices play
- **Configurable Ducking** - Control how much game audio reduces (0-100%)
- **Quick Mute Button** - Toggle voiceovers directly from the dialog interface

### Speech Highlighting
- **Word-by-Word Highlighting** - Dialog text highlights in sync with the spoken audio
- **Customizable Color** - Choose your preferred highlight color

### Quest List Integration
- **[Voiced] Indicator** - See which quests have voice acting directly in the quest list
- **Quest Coverage Stats** - View voiced quest statistics in the plugin panel

### Smart Dialog Matching
- **Fuzzy Text Matching** - Handles dialog variations using Levenshtein distance algorithm
- **Fast Lookups** - SQLite database with indexed queries for instant dialog-to-audio matching

## Installation

Search for **"Quest Voiceover"** in the RuneLite Plugin Hub.

## Supported Quests

Currently **35 quests** are fully voiced, including Tutorial Island, Dragon Slayer I, Waterfall Quest, Lost City, The Restless Ghost, and many more.

<details>
<summary>View full quest list</summary>

- Children of the Sun
- Cook's Assistant
- Current Affairs
- Dwarf Cannon
- Gertrude's Cat
- Ghosts Ahoy
- Imp Catcher
- Misthalin Mystery
- Pandemonium
- Prince Ali Rescue
- Rune Mysteries
- Shades of Mort'ton
- Sheep Shearer
- Spirits of the Elid
- The Restless Ghost
- Tree Gnome Village
- Waterfall Quest
- Witch's House
- Black Knights' Fortress
- Clock Tower
- Doric's Quest
- Dragon Slayer I
- Ernest the Chicken
- Jungle Potion
- Lost City
- Monk's Friend
- Romeo and Juliet
- Scorpion Catcher
- The Depths of Despair
- The Knight's Sword
- The Tourist Trap
- Tribal Totem
- Tutorial Island
- Vampyre Slayer
- Witch's Potion

</details>

## Contributing

Want to help voice more quests? The repository includes a CLI tool that automates voice generation using ElevenLabs.

<details>
<summary>View contribution guide</summary>

### Requirements
- [Pixi](https://pixi.sh/)
- [ElevenLabs Account](https://elevenlabs.io/app)

### Setup
```bash
pixi install
cp .env.example .env  # Add your ElevenLabs API key
```

### Usage
```bash
pixi run cli
```

The interactive CLI will scrape quest transcripts from the OSRS Wiki and guide you through assigning voices to each character.

![Interactive CLI](https://i.imgur.com/DZR3zZT.gif)

**Note:** Player dialog lines use the default `Chris` voice for consistency across all quests.

</details>

## Acknowledgements

Thanks to these projects for inspiration and code snippets:
- [Text to Speech](https://github.com/techgaud/TTS) - Jaco library implementation
- [C Engineer: Completed](https://runelite.net/plugin-hub/show/c-engineer-completed) - Streaming functionality
- [VoiceOver for World of Warcraft](https://github.com/mrthinger/wow-voiceover) - Original inspiration

Special thanks to the RuneLite Discord community and OSRS Wiki contributors.
