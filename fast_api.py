from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from contextlib import asynccontextmanager
from pydantic import BaseModel
import uvicorn
from gtts import gTTS
import os
import time
from difflib import SequenceMatcher
from typing import List

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    # 서버 종료 시 임시 파일들 정리
    for file in os.listdir("temp"):
        if file.startswith("temp_audio_") and file.endswith(".mp3"):
            os.remove(os.path.join("temp", file))

app = FastAPI(lifespan=lifespan)

class GameAnswer(BaseModel):
    user_id: str
    answer: str

class RankingEntry(BaseModel):
    user_id: str
    similarity: float
    rank: int

# 게임 상태를 저장할 전역 변수
game_state = {
    "correct_answer": "멋지다 연진아",
    "audio_file": None,
    "rankings": []
}

@app.post("/game-start/")
async def game_start():
    if not os.path.exists("temp"):
        os.makedirs("temp")

    temp_file = os.path.join("temp", f"temp_audio_{time.time()}.mp3")
    
    try:
        tts = gTTS(text=game_state["correct_answer"], lang='ko')
        tts.save(temp_file)
        game_state['audio_file'] = temp_file
        return {"message": "Game started. Audio file generated."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/records/")
async def records():
    if game_state['audio_file'] is None:
        raise HTTPException(status_code=400, detail="Game not started yet")
    return FileResponse(game_state['audio_file'], media_type="audio/mpeg", filename="output.mp3")

@app.post("/result/")
async def result(answer: GameAnswer):
    try:
        if game_state['audio_file'] is None:
            raise HTTPException(status_code=400, detail="Game not started yet")
        
        similarity = SequenceMatcher(None, game_state['correct_answer'], answer.answer).ratio()
        
        # 랭킹에 새로운 결과 추가
        game_state['rankings'].append({"user_id": answer.user_id, "similarity": similarity})
        
        # 유사도를 기준으로 내림차순 정렬
        game_state['rankings'].sort(key=lambda x: x['similarity'], reverse=True)
        
        # 랭킹 부여
        for i, entry in enumerate(game_state['rankings']):
            entry['rank'] = i + 1

        # 현재 사용자의 랭킹 찾기
        user_ranking = next(entry for entry in game_state['rankings'] if entry['user_id'] == answer.user_id)

        result = {
            "similarity": similarity,
            "user_answer": answer.answer,
            "correct_answer": game_state['correct_answer'],
            "user_ranking": user_ranking['rank'],
            "total_players": len(game_state['rankings'])
        }

        return JSONResponse(content=result)
    except Exception as e:
        print(f"Error in result: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/rankings/", response_model=List[RankingEntry])
async def get_rankings():
    return game_state['rankings']

@app.post("/reset-game/")
async def reset_game():
    game_state['audio_file'] = None
    game_state['rankings'] = []
    return JSONResponse(content={"message": "Game reset. Rankings cleared."})

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="info")
