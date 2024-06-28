import hashlib

def str_to_md5(text: str) -> str:
    hash_object = hashlib.md5(text.encode())
    return hash_object.hexdigest()

def remove_special_characters(line: str) -> str:
    line.replace("[player name]", "")
    line.replace("[1-19]", "")
    line.replace("[boy/girl]", "boy")
    line.replace("[#]", "")
    line.replace("[ball/balls]", "")
    line.replace("[lad/lass]", "lad")
    return line

def escape_single_quotes(text):
    return text.replace("'", "''")