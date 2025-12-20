import os
import sqlite3
from sqlite3 import Connection
import voiceover_cli.utils as utils

#! This needs to be changed each time we are updating the DB.

DATABASE_NAME = "quest_voiceover"
OUTPUT_DIR = "output_db"

def create_connection() -> Connection:
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    return sqlite3.connect(f"{OUTPUT_DIR}/{DATABASE_NAME}.db", check_same_thread=False)

def init_virtual_table(connection: Connection) -> None:
    cursor = connection.cursor()
    query = '''
        CREATE VIRTUAL TABLE IF NOT EXISTS dialogs USING fts4(
            quest TEXT NOT NULL UNINDEXED,
            character TEXT NOT NULL,
            text TEXT NOT NULL,
            uri TEXT NOT NULL UNINDEXED
        )
    '''

    cursor.execute(query)
    connection.commit()

def insert_quest_voiceover(connection: Connection,quest: str, character: str, text: str, file_name: str) -> None:
    cursor = connection.cursor()
    query = f'''
        INSERT OR REPLACE INTO dialogs(quest, character, text, uri) VALUES ('{utils.escape_single_quotes(quest)}', '{utils.escape_single_quotes(character)}', '{utils.escape_single_quotes(text)}', '{file_name}');
    '''

    cursor.execute(query)
    connection.commit()