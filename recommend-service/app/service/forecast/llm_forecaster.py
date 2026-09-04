"""
OpenAI Responses API + web_search 기반 수요 예측.

실패·타임아웃 시 예외를 던지지 않고 None을 돌려준다. 호출부가 SYNTHETIC_TREND로 폴백한다.
추천은 비핵심 기능이라 외부 API가 죽어도 화면은 떠야 한다.
"""

import hashlib
import json
import logging
import time
from datetime import date
from pathlib import Path
from typing import Optional

from app.config.settings import settings

logger = logging.getLogger(__name__)

BASIS_LLM = "LLM_FORECAST"

DEMAND_CHANGE_MIN = -0.5
DEMAND_CHANGE_MAX = 1.0

# Structured Outputs 스키마. strict 모드라 모든 필드가 required여야 한다.
RESPONSE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["demand_change", "confidence", "factors", "summary"],
    "properties": {
        "demand_change": {
            "type": "number",
            "description": f"평상시 대비 수요 변화율. {DEMAND_CHANGE_MIN}~{DEMAND_CHANGE_MAX}",
        },
        "confidence": {"type": "number", "description": "0~1"},
        "summary": {"type": "string", "description": "한국어 두 문장 이내 요약"},
        "factors": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["name", "trend", "impact", "evidence", "source_url"],
                "properties": {
                    "name": {"type": "string"},
                    "trend": {"type": "string", "enum": ["RISING", "FALLING", "FLAT"]},
                    "impact": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"]},
                    "evidence": {"type": "string"},
                    "source_url": {"type": "string"},
                },
            },
        },
    },
}


def _season(as_of: date) -> str:
    return {12: "겨울", 1: "겨울", 2: "겨울", 3: "봄", 4: "봄", 5: "봄",
            6: "여름", 7: "여름", 8: "여름", 9: "가을", 10: "가을", 11: "가을"}[as_of.month]


def _prompt(drug: str, category: str, as_of: date, days: int) -> str:
    return (
        f"오늘은 {as_of.isoformat()}이고 계절은 {_season(as_of)}이다.\n"
        f"대상 의약품: {drug}\n"
        f"약효군: {category}\n\n"
        f"현재 국내 감염병 유행 상황(질병관리청 표본감시 등)과 계절 요인을 웹에서 확인하고, "
        f"향후 {days}일 동안 이 의약품의 수요가 평상시 대비 몇 % 변할지 예측하라.\n\n"
        f"규칙:\n"
        f"- demand_change는 비율이다. 0.3이면 +30%, -0.2면 -20%. "
        f"{DEMAND_CHANGE_MIN} 이상 {DEMAND_CHANGE_MAX} 이하.\n"
        f"- factors는 최소 1개. 각 근거에 실제 확인한 출처 URL을 넣어라.\n"
        f"- summary는 한국어로 두 문장 이내.\n"
        f"- JSON만 출력한다."
    )


# ---------------------------------------------------------------- 캐시

def _cache_path(category: str, as_of: date, days: int) -> Path:
    key = f"{category}|{as_of.isoformat()}|{days}|{settings.openai_model}"
    digest = hashlib.sha256(key.encode("utf-8")).hexdigest()[:16]
    return Path(settings.forecast_cache_dir) / f"{digest}.json"


def _read_cache(path: Path) -> Optional[dict]:
    if not path.exists():
        return None
    age_hours = (time.time() - path.stat().st_mtime) / 3600
    if age_hours > settings.forecast_cache_ttl_hours:
        logger.info(f"[LLM] 캐시 만료 ({age_hours:.1f}h) - {path.name}")
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as e:
        logger.warning(f"[LLM] 캐시 읽기 실패 - {e}")
        return None


def _write_cache(path: Path, payload: dict) -> None:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    except OSError as e:
        logger.warning(f"[LLM] 캐시 저장 실패 - {e}")


# ---------------------------------------------------------------- 검증

def _validate(data: dict) -> Optional[dict]:
    """범위·필수 항목을 확인한다. 어긋나면 None (→ 폴백)."""
    try:
        change = float(data["demand_change"])
    except (KeyError, TypeError, ValueError):
        logger.warning("[LLM] demand_change 없음/형식 오류")
        return None

    if not DEMAND_CHANGE_MIN <= change <= DEMAND_CHANGE_MAX:
        logger.warning(f"[LLM] demand_change 범위 이탈: {change}")
        return None

    factors = data.get("factors") or []
    if not factors:
        logger.warning("[LLM] factors가 비어 있음")
        return None

    confidence = data.get("confidence")
    try:
        confidence = min(1.0, max(0.0, float(confidence)))
    except (TypeError, ValueError):
        confidence = None

    return {
        "demandChange": round(change, 4),
        "confidence": confidence,
        "summary": (data.get("summary") or "").strip(),
        "factors": [
            {
                "name": str(f.get("name", "")),
                "trend": f.get("trend") if f.get("trend") in ("RISING", "FALLING", "FLAT") else "FLAT",
                "impact": f.get("impact") if f.get("impact") in ("HIGH", "MEDIUM", "LOW") else "MEDIUM",
                "evidence": str(f.get("evidence", "")),
                "source_url": str(f.get("source_url", "")),
            }
            for f in factors
        ],
        "basis": BASIS_LLM,
    }


# ---------------------------------------------------------------- 호출

def forecast(drug: str, category: str, as_of: date, days: int) -> Optional[dict]:
    """
    성공하면 {demandChange, confidence, summary, factors, basis}, 실패하면 None.
    캐시 키는 (약효군, 기준일, 기간)이다. 원료가 달라도 약효군이 같으면 재사용한다.
    """
    if not settings.openai_api_key:
        logger.warning("[LLM] OPENAI_API_KEY 미설정 - 폴백")
        return None

    path = _cache_path(category, as_of, days)
    cached = _read_cache(path)
    if cached:
        logger.info(f"[LLM] 캐시 적중 - {category} {as_of} {days}일")
        return cached

    try:
        from openai import OpenAI
    except ImportError:
        logger.warning("[LLM] openai 패키지 없음 - 폴백")
        return None

    client = OpenAI(api_key=settings.openai_api_key, timeout=settings.openai_timeout)
    prompt = _prompt(drug, category, as_of, days)

    started = time.monotonic()
    try:
        response = client.responses.create(
            model=settings.openai_model,
            input=prompt,
            temperature=0,
            tools=[{"type": "web_search"}],
            text={
                "format": {
                    "type": "json_schema",
                    "name": "demand_forecast",
                    "schema": RESPONSE_SCHEMA,
                    "strict": True,
                }
            },
        )
    except Exception as e:                       # 타임아웃·인증·쿼터 등 전부
        elapsed = time.monotonic() - started
        logger.warning(f"[LLM] 호출 실패 ({elapsed:.1f}s) - {type(e).__name__}: {e}")
        return None

    elapsed = time.monotonic() - started
    raw = (response.output_text or "").strip()
    if not raw:
        logger.warning("[LLM] 빈 응답")
        return None

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        logger.warning(f"[LLM] JSON 파싱 실패 - {raw[:200]}")
        return None

    result = _validate(data)
    if result is None:
        return None

    logger.info(
        f"[LLM] 예측 완료 ({elapsed:.1f}s) - {category} {as_of} {days}일, "
        f"demandChange={result['demandChange']:+.2%}, factors={len(result['factors'])}"
    )
    _write_cache(path, result)
    return result
