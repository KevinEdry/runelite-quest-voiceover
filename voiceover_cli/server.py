from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import voiceover_cli.database as db

app = FastAPI(title="Quest Voiceover DB API")

connection = None

class DialogEntry(BaseModel):
    quest: str
    character: str
    text: str
    file_name: str

@app.on_event("startup")
def startup():
    global connection
    connection = db.create_connection()
    db.init_table(connection)

@app.on_event("shutdown")
def shutdown():
    global connection
    if connection:
        connection.close()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/dialog")
def insert_dialog(entry: DialogEntry):
    try:
        db.insert_quest_voiceover(
            connection,
            entry.quest,
            entry.character,
            entry.text,
            entry.file_name
        )
        return {"status": "inserted", "entry": entry.model_dump()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
