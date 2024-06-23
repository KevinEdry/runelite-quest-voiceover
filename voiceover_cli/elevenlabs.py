

import os
from typing import List
from elevenlabs.client import ElevenLabs
from elevenlabs import VoiceSettings, Voice, play, stream, save
import voiceover_cli.utils as utils


ELEVENLABS_API_BASE_URL = "https://api.elevenlabs.io/v1"
OUTPUT_DIR = "voiceover_output"

class ElevenlabsSDK:
    def __init__(self) -> None:
        self.api_key = os.getenv('ELEVENLABS_API_KEY')
        self.client = ElevenLabs(
            api_key=self.api_key
        )

    def get_voices(self) -> List[Voice]:
        voices = self.client.voices.get_all().voices
        return voices
    
    def generate(self, character: str, voice_id: str, line: str, next_line: str | None, previous_line: str | None) -> None:
        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)

        unique_id = utils.str_to_md5(f'{character}|{line}')
        mp3_filename = os.path.join(OUTPUT_DIR, f"{unique_id}.mp3")

        if not os.path.exists(mp3_filename):
            try:
                request_options = {
                    "additional_body_parameters": {
                        "previous_text": None if previous_line == None else previous_line,
                        "next_text": None if next_line == None else next_line,
                    }
                }
                audio = self.client.generate(text=line.strip(), 
                                             voice=Voice(
                                                 voice_id=voice_id,
                                                 settings=VoiceSettings(stability=0.3, style=0.4, similarity_boost=0.7),
                                             ),
                                             model="eleven_multilingual_v2", 
                                             output_format="mp3_44100_96",
                                             request_options=request_options)
                
                save(audio, mp3_filename)
            except Exception as e:
                print(f"Error generating voice-over for line '{line}': {e}")
    