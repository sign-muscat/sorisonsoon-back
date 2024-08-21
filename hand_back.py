from fastapi import FastAPI, File, UploadFile, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, Enum, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session, relationship
import torch
import torch.nn as nn
from torchvision import transforms, models
from PIL import Image
import io
import yaml
import logging

# 로깅 설정
logging.basicConfig(level=logging.WARNING)

# 데이터베이스 설정
DATABASE_URL = "mysql+pymysql://admin:thflthsrmf1@sorisonsoon.cxw0iu6oc8k4.ap-northeast-2.rds.amazonaws.com:3306/sorisonsoon"
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# 데이터베이스 모델
class GameRiddle(Base):
    __tablename__ = "game_riddle"

    riddle_id = Column(Integer, primary_key=True, index=True)
    question = Column(String(255), nullable=False)
    video = Column(String(255), nullable=True)
    category = Column(Enum("DAILY_LIFE", "EMOTION", "ANIMALS_PLANTS", "JOB", "FOOD_CLOTHING", "PLACE", "ETC"), nullable=False)
    difficulty = Column(Enum("LEVEL_1", "LEVEL_2", "LEVEL_3"), nullable=False)
    label = Column(String(255), nullable=False)
    
    steps = relationship("GameRiddleStep", back_populates="riddle")

class GameRiddleStep(Base):
    __tablename__ = "game_riddle_step"

    riddle_step_id = Column(Integer, primary_key=True, index=True)
    riddle_id = Column(Integer, ForeignKey('game_riddle.riddle_id'))
    step = Column(Integer, nullable=False)
    answer = Column(String(255), nullable=False)

    riddle = relationship("GameRiddle", back_populates="steps")

# FastAPI 앱 생성
app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 데이터베이스 의존성
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# GPU 설정
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
logging.warning(f"Using device: {device}")

# 모델 로드 함수
def load_model():
    model = models.resnet50(weights=None)
    num_ftrs = model.fc.in_features
    with open("data.yaml", 'r') as stream:
        data = yaml.safe_load(stream)
    num_classes = len(data['names'])
    model.fc = nn.Linear(num_ftrs, num_classes)
    model.load_state_dict(torch.load('model/best_resnet_multilabel_model.pth', map_location=device))
    model.to(device)
    model.eval()
    return model, data['names']

model, class_names = load_model()

# 이미지 변환
transform = transforms.Compose([
    transforms.Resize((640, 640)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

# 요청 모델
class PredictionRequest(BaseModel):
    riddle_id: int
    current_step: int

# 예측 결과 모델
class PredictionResponse(BaseModel):
    is_correct: bool
    current_step: int
    total_steps: int
    feedback: str

# 이미지 예측 엔드포인트
from fastapi import Form

@app.post("/api/v1/sign/predict", response_model=PredictionResponse)
async def predict_image(
    file: UploadFile = File(...), 
    riddle_id: int = Form(...), 
    current_step: int = Form(...), 
    db: Session = Depends(get_db)
):
    try:
        # 데이터베이스에서 현재 단계 정보 조회
        current_step_obj = db.query(GameRiddleStep).filter(
            GameRiddleStep.riddle_id == riddle_id,
            GameRiddleStep.step == current_step
        ).first()

        if not current_step_obj:
            raise HTTPException(status_code=400, detail="Invalid riddle or step")

        # 전체 단계 수 조회
        total_steps = db.query(GameRiddleStep).filter(
            GameRiddleStep.riddle_id == riddle_id
        ).count()

        # 이미지 처리 및 예측
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        image = transform(image).unsqueeze(0).to(device)

        with torch.no_grad():
            outputs = model(image)
            probabilities = torch.sigmoid(outputs)
            max_prob, predicted_idx = torch.max(probabilities, 1)
            predicted_label = class_names[predicted_idx.item()]

        # 정답 확인
        correct_label = f"{current_step_obj.riddle.label}_{current_step}"
        is_correct = (predicted_label == correct_label)

        feedback = "정답입니다!" if is_correct else "틀렸습니다. 다시 시도해보세요."

        next_step = current_step + 1 if is_correct else current_step

        if next_step > total_steps:
            feedback = "모든 단계를 완료했습니다!"

        return PredictionResponse(
            is_correct=is_correct,
            current_step=current_step,
            total_steps=total_steps,
            feedback=feedback
        )
    
    except Exception as e:
        logging.error(f"Error in predict_image: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)