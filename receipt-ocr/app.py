import json
import logging
import os
from io import BytesIO
import tempfile

from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image
from paddleocr import PaddleOCR
import requests

logger = logging.getLogger("receipt-ocr")

MAX_FILE_SIZE = 5 * 1024 * 1024
ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"}
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "").strip()

app = FastAPI(title="DongBang Receipt OCR")
ocr = PaddleOCR(lang="korean", use_doc_orientation_classify=True,
                use_doc_unwarping=True, use_textline_orientation=True)


import time

CANDIDATE_MODELS = [
    "gemini-3.6-flash",
    "gemini-3-flash-preview",
]


def correct_with_gemini(raw_text: str, api_key: str) -> dict:
    if not api_key or not raw_text.strip():
        return None

    prompt = f"""당신은 대한민국 영수증(식당, 카페, 마트, 다이소, 문구점, 대관시설 등) 전문 분석 및 회계 데이터 추출 AI입니다.
아래는 영수증에서 1차 OCR로 추출된 원본 텍스트입니다. 인쇄 흐림, 폰트 깨짐, 줄바꿈 오류 등으로 인한 오타가 포함되어 있을 수 있습니다.

[수행 작업]
1. 영수증 텍스트를 분석하여 오타를 한국어 상식 및 상점 문맥에 맞게 자연스럽게 교정하고 가독성 좋게 정리하세요.
2. 동아리/단체 회계 장부 등록을 위해 아래 JSON 규격에 맞춰 핵심 항목을 정확히 추출하세요.

[JSON 필드 추출 및 분류 가이드]
- "itemTitle": 영수증 대표 품목 요약명 (예: '아메리카노 외 3건', '치즈계란말이 외 25건', '사무용품 세트' 등 40자 이내)
- "spentOn": 결제/거래 일자 (YYYY-MM-DD 형식, 영수증 날짜 기준. 없으면 null)
- "amount": 실제 결제된 최종 합계 금액 숫자 (할인 반영된 실결제액. 콤마 없는 정수. 없으면 null)
- "merchant": 상호명/가맹점명 (지점명 포함 가능. 부가 설명 제외하고 상호명만. 없으면 null)
- "category": 영수증 품목 및 사용처 맥락에 따른 추천 분류. 반드시 다음 6개 중 하나만 선택:
  * "행사 운영" : 회식, 뒤풀이, 총회/행사 식음료, 부스 운영비 등
  * "공간 대관" : 스터디룸, 파티룸, 연습실, 체육시설 대여 등
  * "운영비" : 정기 모임 음료/다과, 회의비, 일상 소모품 등
  * "물품 구매" : 다이소, 마트 비품, 문구류, 보드게임, 비품/장비 구매 등
  * "홍보비" : 포스터 인쇄, 배너/현수막 제작, 홍보물 인쇄 등
  * "기타" : 위 분류에 속하지 않는 경우
- "paymentMethod": 결제 수단. 반드시 다음 4개 중 하나만 선택:
  * "동아리 카드" : 신용카드/체크카드 승인 표기(카드번호 마스킹, 승인번호 등)가 있는 경우
  * "현금" : 현금영수증(소득공제/지출증빙) 표기가 있는 경우
  * "계좌이체" : 계좌이체/무통장입금 표기가 있는 경우
  * "기타" : 결제 수단 확인 불가 시
- "memo": 주요 품목 및 수량 요약 (예: '치즈계란말이, 김치찌개 등 26개 품목')
- "items": 개별 품목 목록 [{{"name": "품목명", "quantity": 수량(정수), "price": 금액(정수)}}]
- "correctedText": 오타 교정 및 줄바꿈이 정돈된 전체 영수증 텍스트

반환할 JSON 스키마:
{{
  "itemTitle": "대표 품목 요약",
  "spentOn": "YYYY-MM-DD",
  "amount": 10000,
  "merchant": "상호명",
  "category": "행사 운영",
  "paymentMethod": "동아리 카드",
  "memo": "품목 요약 메모",
  "items": [
    {{"name": "품목명", "quantity": 1, "price": 10000}}
  ],
  "correctedText": "교정된 전체 텍스트"
}}

영수증 1차 OCR 텍스트:
{raw_text}
"""
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "thinkingConfig": {
                "thinkingBudget": 0
            }
        }
    }

    for model_name in CANDIDATE_MODELS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"
        for attempt in range(2):
            try:
                res = requests.post(url, json=payload, timeout=25)
                if res.status_code == 200:
                    data = res.json()
                    text_content = data["candidates"][0]["content"]["parts"][0]["text"]
                    logger.info(f"Successfully processed receipt with {model_name}")
                    return json.loads(text_content)
                elif res.status_code in (503, 429):
                    logger.warning(
                        f"Gemini {model_name} attempt {attempt + 1} returned {res.status_code}, trying fallback..."
                    )
                    time.sleep(0.3)
                    continue
                else:
                    logger.warning(f"Gemini API ({model_name}) returned status {res.status_code}: {res.text}")
                    break
            except requests.exceptions.Timeout:
                logger.warning(f"Gemini API ({model_name}) timed out, trying next model...")
                break
            except Exception as e:
                logger.warning(f"Failed to call Gemini API ({model_name}): {e}")
                break

    return None


@app.get("/health")
def health():
    return {"status": "UP"}


@app.post("/ocr")
async def recognize_receipt(file: UploadFile = File(...)):
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(status_code=415, detail="JPEG, PNG, WebP만 지원합니다.")

    content = await file.read(MAX_FILE_SIZE + 1)
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(status_code=413, detail="이미지는 5MB 이하만 업로드할 수 있습니다.")

    try:
        image = Image.open(BytesIO(content))
        image.verify()
        suffix = {
            "image/jpeg": ".jpg",
            "image/png": ".png",
            "image/webp": ".webp",
        }[file.content_type]
        with tempfile.NamedTemporaryFile(suffix=suffix) as temp:
            temp.write(content)
            temp.flush()
            result = ocr.predict(input=temp.name)
            blocks = []
            for page in result:
                data = page.json
                data = data.get("res", data)
                for text, score in zip(data.get("rec_texts", []), data.get("rec_scores", [])):
                    blocks.append({"text": text, "confidence": float(score)})

        raw_text = "\n".join(block["text"] for block in blocks)

        # Gemini LLM Post-Processing
        gemini_data = correct_with_gemini(raw_text, GEMINI_API_KEY)
        if gemini_data:
            merchant = gemini_data.get("merchant")
            spent_on = gemini_data.get("spentOn")
            amount = gemini_data.get("amount")
            item_title = gemini_data.get("itemTitle")
            category = gemini_data.get("category") or "기타"
            payment_method = gemini_data.get("paymentMethod") or "동아리 카드"
            memo = gemini_data.get("memo")
            items = gemini_data.get("items", [])
            corrected_text = gemini_data.get("correctedText") or raw_text

            return {
                "text": corrected_text,
                "itemTitle": item_title,
                "spentOn": spent_on,
                "amount": amount,
                "merchant": merchant,
                "category": category,
                "paymentMethod": payment_method,
                "memo": memo,
                "items": items,
                # 하위 호환 필드
                "storeName": merchant,
                "purchasedAt": spent_on,
                "totalAmount": amount,
                "blocks": blocks
            }

        return {
            "text": raw_text,
            "itemTitle": None,
            "spentOn": None,
            "amount": None,
            "merchant": None,
            "category": "기타",
            "paymentMethod": "동아리 카드",
            "memo": None,
            "items": [],
            "storeName": None,
            "purchasedAt": None,
            "totalAmount": None,
            "blocks": blocks
        }

    except Exception as exc:
        logger.error(f"OCR processing failed: {exc}", exc_info=True)
        raise HTTPException(status_code=422, detail="영수증 이미지를 인식할 수 없습니다.") from exc
