import hashlib


def str_to_md5(text: str) -> str:
    hash_object = hashlib.md5(text.encode())
    return hash_object.hexdigest()
