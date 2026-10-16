"""Small, bounded Gemini client for optional receipt post-processing."""

import json
import logging
import re
import time
from datetime import date

import requests


logger = logging.getLogger("receipt-ocr")
DEFAULT_MODEL = "gemini-3-flash-preview"
RETRYABLE_STATUS = {429, 500, 503, 504}
CATEGORIES = {"행사 운영", "공간 대관", "운영비", "물품 구매", "홍보비", "기타"}
PAYMENT_METHODS = {"동아리 카드", "현금", "계좌이체", "기타"}


def generate_json(prompt, api_key, model=DEFAULT_MODEL):
    if not api_key:
        return None
    if not re.fullmatch(r"[A-Za-z0-9._-]+", model):
        logger.warning("Invalid Gemini model name configured")
        return None

    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "thinkingConfig": {"thinkingLevel": "low"},
        },
    }
    for attempt in range(2):
        try:
            response = requests.post(
                url,
                headers={"x-goog-api-key": api_key},
                json=payload,
                timeout=(3, 12),
            )
            if response.status_code == 200:
                parts = response.json()["candidates"][0]["content"]["parts"]
                result = json.loads("".join(part.get("text", "") for part in parts))
                if isinstance(result, dict):
                    return result
                logger.warning("Gemini receipt response is not a JSON object")
                return None
            logger.warning("Gemini receipt request failed: model=%s status=%s", model, response.status_code)
            if response.status_code not in RETRYABLE_STATUS:
                return None
        except (requests.RequestException, ValueError, KeyError, IndexError, TypeError) as exc:
            logger.warning("Gemini receipt request failed: model=%s error=%s", model, type(exc).__name__)
            if not isinstance(exc, requests.RequestException):
                return None
        if attempt == 0:
            time.sleep(0.5)
    return None


def normalize_receipt_data(data):
    """Reject malformed AI values instead of saving invented or unparsable fields."""
    if not isinstance(data, dict):
        return None

    def optional_text(key, limit):
        value = data.get(key)
        return value.strip()[:limit] if isinstance(value, str) and value.strip() else None

    spent_on = optional_text("spentOn", 10)
    try:
        if spent_on and re.fullmatch(r"\d{4}-\d{2}-\d{2}", spent_on):
            date.fromisoformat(spent_on)
        else:
            spent_on = None
    except ValueError:
        spent_on = None

    amount = data.get("amount")
    if isinstance(amount, bool) or not isinstance(amount, int) or not 0 < amount < 10**13:
        amount = None

    category = data.get("category")
    payment_method = data.get("paymentMethod")
    items = []
    if isinstance(data.get("items"), list):
        for item in data["items"][:100]:
            if not isinstance(item, dict):
                continue
            name, quantity, price = item.get("name"), item.get("quantity"), item.get("price")
            if (isinstance(name, str) and name.strip()
                    and type(quantity) is int and quantity > 0
                    and type(price) is int and price >= 0):
                items.append({"name": name.strip()[:100], "quantity": quantity, "price": price})

    return {
        "itemTitle": optional_text("itemTitle", 80),
        "spentOn": spent_on,
        "amount": amount,
        "merchant": optional_text("merchant", 80),
        "category": category if isinstance(category, str) and category in CATEGORIES else "기타",
        "paymentMethod": payment_method if isinstance(payment_method, str) and payment_method in PAYMENT_METHODS else "기타",
        "memo": optional_text("memo", 300),
        "items": items,
        "correctedText": optional_text("correctedText", 20000),
    }
