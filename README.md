![](https://runelite.net/img/logo.png)
# Quest Voiceover
This plugin aims to provide voice over acting to the brilliant quest writing in Old School Runescape as a Runelite plugin.
<br>
<br>
We want to add support to most (if not all) quests in the game, but this will be an ongoing process to do right, if you want to help us out, feel free to join the discord and message us, lets see this through.

![Discord Banner 2](https://discord.com/api/guilds/1254623476086472758/widget.png?style=banner2)

## Supported Quests
While not all the voices are acted in the best way, with our cli tooling, we are hoping to cover most of the quests in the game asap, this is our progress so far:
- [x] Cook's Assistant
- [x] Sheep Shearer
- [x] Misthalin Mystery
- [x] Prince Ali Rescue
- [ ] The Restless Ghost
- [ ] Rune Mysteries
- [ ] Imp Catcher
- [ ] Witch's Potion
- [ ] Gertrude's Cat

If you want to see this plugin support other quests you can either:
1. Fork this repo and use the `voiceover_cli` to generate the sounds and contribute.
2. Send us a message in our discord server.

## Generating Voiceovers
In this repository contains the tools to contribute!
<br>
This can be done by using our `voiceover_cli` tool to generate all the sound bites automatically for a given quest.

### Requirements
- Python 3.8+
- Poetry - https://python-poetry.org/docs/
- Elevenlabs Account - https://elevenlabs.io/app

### installation
1. Make a python virtual environment with poetry.
```bash
poetry shell
```
2. Install the required packages.
```bash
poetry install
```
3. Copy the .env.example file to .env and fill in your ElevenLabs API Key (You can skip this step if you want to CLI to automate it).
```bash
cp .env.example .env
```

### Usage
1. In Elevenlabs, create a voice for each specific character in the quest, you can do this by searching for a suitable voice from their voice library or making your own. <br>
   **Note: To keep the voice actors consistent, the player dialog lines will always be voiced by the default `Chris` voice!**
2. To use the interactive cli tool, run the following command:
```bash
python cli-main.py
```

3. An interactive cli tool should open for you to generate the sounds, read through the instructions and generate the transcript voiceover!

![Interactive CLI](https://i.imgur.com/DZR3zZT.gif)
<br>

This tool will automatically scrape the Old School Runescape Wiki for the available quest transcripts and characters and will ask you to assign a voice for each one based on the voices you created in step 1.
<br> After the voices are done generating, fork the `sounds` branch, and add the files to the root directory before  creating a pull request.

## Acknowledgements
Huge thanks to the following runelite plugin repositories for helping out with code snippets and implementation.
- [Text to speech](https://github.com/techgaud/TTS) - Thanks for helping with the Jaco library implementation
- [C Engineer: Completed](https://runelite.net/plugin-hub/show/c-engineer-completed) - Thanks for helping with the streaming functionality of the voiceover files.
- [VoiceOver for World of Warcraft](https://github.com/mrthinger/wow-voiceover) - Thanks for the initial inspiration for this project.

I also can't stress enough how **instrumental** and helpful the Runelite developers discord community and the OSRS Wiki community were to this project.