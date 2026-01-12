import os
from prompt_toolkit.shortcuts import checkboxlist_dialog, radiolist_dialog, yes_no_dialog, input_dialog
import voiceover_cli.wiki_utils as wiki_utils
from voiceover_cli.elevenlabs import ElevenlabsSDK
from tqdm import tqdm
import voiceover_cli.database as database

from dotenv import load_dotenv
load_dotenv()

ELEVENLABS_API_KEY = os.getenv('ELEVENLABS_API_KEY')

def prompt_user():

    # Scrape OSRS Wiki for the available quest transcripts.
    quest_transcripts = wiki_utils.get_quests()
    quest_transcripts_tuple = [(item['idx'], item['title']) for item in quest_transcripts]

    selected_quest = radiolist_dialog(
        title="Select Quest",
        text="Select quest transcript to generate voiceover.",
        values=quest_transcripts_tuple
    ).run()
    if selected_quest == None: return

    quest: wiki_utils.QuestTranscriptMetadata = next(filter(lambda quest: quest['idx'] == selected_quest, quest_transcripts))
    print(f"You choose: {quest['title']} | {quest['link']}")

    # Make sure we have the Elevenlabs API key.
    if ELEVENLABS_API_KEY == None:
        api_key_input = input_dialog(
            title="Elevenlabs API Key",
            text="It seems you don't have a `.env` file with the `ELEVENLABS_API_KEY` property.\n"
                f"Paste here your Elevenlabs api key to create a new `.env` file.\n\n"
                f"See: https://elevenlabs.io/docs/workspace/overview#managing-api-keys",
            password=True
        ).run()
        if api_key_input is None: return

        with open('../osrs-voiceover-generator/.env', 'w') as env_file:
            env_file.write(f'ELEVENLABS_API_KEY={api_key_input}')
        load_dotenv()


    quest_characters_array = wiki_utils.get_quest_characters(quest["link"])
    selected_characters = checkboxlist_dialog(
        title="Choose Quest Characters",
        text="Choose which characters do you want to generate voice lines for.",
        values=[(item, item) for item in quest_characters_array],
        default_values=quest_characters_array
    ).run()
    if selected_characters is None or len(selected_characters) == 0: return;
        
    # Scrape the OSRS Wiki for the quest transcripts themselves.
    quest_transcript_dict: wiki_utils.QuestTranscript = wiki_utils.get_transcript(quest['link'], selected_characters);
    quest_transcript = quest_transcript_dict['transcript']

    # We need this to provide the original lines order as context when Elevenlabs generates voiceovers.
    flatten_quest_transcript = quest_transcript_dict['flattened_transcript']

    # Initialize the Elevenlabs class
    elevenlabs = ElevenlabsSDK()

    # Get the available voices from the Elevenlabs SDK.
    voices_list = elevenlabs.get_voices()
    voices_tuple = [(voice.voice_id, voice.name) for voice in voices_list]

    character_voices_dict = {}
    dialog_lines_counter = 0
    confirmation_dialog_info = ""

    # Ask the user to pick a specific voice for each character in the quest.
    for key in quest_transcript:
        selected_voice = radiolist_dialog(
            title=f"Choose a Voice: {key}",
            text=f"Please choose a voice that will act out the character {key}.",
            values=voices_tuple
        ).run()
        if selected_voice == None: return

        character_voices_dict[key] = selected_voice
        dialog_lines_counter += len(quest_transcript[key])
        selected_voice_name = next(filter(lambda voice: voice.voice_id == selected_voice, voices_list)).name
        confirmation_dialog_info += f"{key}: Voice - {selected_voice_name} | Lines - {len(quest_transcript[key])}\n"
    
    # Confirmation dialog before we generate the voicelines.
    confirmed = yes_no_dialog(
        title=f"Confirm Voiceover Generation: {quest['title'].replace("Transcript:", "")}",
        text=f'{quest['title'].replace("Transcript:", "")} ({quest["link"]})\n'
            f'Total dialog lines: {dialog_lines_counter}\n\n'
            f'Characters:\n'
            f'{confirmation_dialog_info}\n'
            f'By pressing YES you will start the voiceover generation!'
    ).run() 
    if confirmed is None or confirmed is False: return

    # Show progressbar while generating voicelines with Elevenlabs.
    progress = tqdm(flatten_quest_transcript, desc=f"Generating {quest['title'].replace("Transcript:", "")} Voiceover")
    for idx, (character, line) in enumerate(progress):
        progress.write(f'[{idx+1}] Current Line: {line}')

        previous_line = flatten_quest_transcript[idx-1][1] if idx > 0 else None
        next_line = flatten_quest_transcript[idx-1][1] if idx < len(flatten_quest_transcript)-1 else None
        
        try:
            connection = database.create_connection()
            database.init_table(connection=connection)

            file_name = elevenlabs.generate(character=character, 
                                voice_id=character_voices_dict[character], 
                                line=line,
                                next_line=next_line,
                                previous_line=previous_line)
            if file_name is None: return

            database.insert_quest_voiceover(connection=connection, quest=quest['title'].replace("Transcript:", ""), character=character, text=line, file_name=file_name)
        except Exception as e:
            print(f"Error generating voice-over for line '{line}': {e}")
        
    
prompt_user()