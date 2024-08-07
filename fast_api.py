from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from contextlib import asynccontextmanager
from pydantic import BaseModel
import uvicorn
from gtts import gTTS
import os
import time

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    # 서버 종료 시 임시 파일들 정리
    for file in os.listdir("temp"):
        if file.startswith("temp_audio_") and file.endswith(".mp3"):
            os.remove(os.path.join("temp", file))

app = FastAPI(lifespan=lifespan)

class TextInput(BaseModel):
    text: str

@app.post("/text-to-speech/")
async def text_to_speech(input: TextInput):
    if not os.path.exists("temp"):
        os.makedirs("temp")
    
    temp_file = os.path.join("temp", f"temp_audio_{time.time()}.mp3")

    try:
        print(f"Generating audio for text: {input.text}")
        tts = gTTS(text=input.text, lang='ko')
        tts.save(temp_file)
        print(f"Audio file saved at: {temp_file}")

        if not os.path.exists(temp_file):
            raise FileNotFoundError(f"Failed to create file: {temp_file}")

        file_size = os.path.getsize(temp_file)
        print(f"File size: {file_size} bytes")

        if file_size == 0:
            raise ValueError(f"File is empty: {temp_file}")

        return FileResponse(temp_file, media_type="audio/mpeg", filename="output.mp3")

    except Exception as e:
        print(f"Error in text_to_speech: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="info")