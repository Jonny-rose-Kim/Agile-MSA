"""
수요 예측기 선택.

FORECASTER=llm  → OpenAI Responses API + web_search (실패 시 자동 폴백)
FORECASTER=mock → 합성 데이터 추세 (SYNTHETIC_TREND). OpenAI 호출 없음

기존 합성 추세 코드(forecast_service.predict_demand)는 지우지 않고 폴백 경로로 남겨뒀다.
"""

import logging
from datetime import date
from typing import List, Optional, Sequence

from app.config.settings import settings
from app.service import forecast_service
from app.service.forecast import llm_forecaster

logger = logging.getLogger(__name__)


def _synthetic(material_code: str, materials: Sequence[dict], days: int, as_of: date) -> dict:
    """합성 데이터 추세를 LLM 예측기와 같은 형태로 맞춰 돌려준다."""
    prediction = forecast_service.predict_demand(material_code, materials, days, as_of)
    raw_factors = forecast_service.get_factors(as_of)

    factors = [
        {
            "name": f["name"],
            "trend": f["trend"],
            "impact": f["impact"],
            "evidence": (
                f"최근 12주 {f['name']} 추이 (마지막 관측 "
                f"{f['recent'][-1]['value'] if f['recent'] else '-'})"
            ),
            "source_url": "",          # 합성 데이터라 출처가 없다
        }
        for f in raw_factors
    ]

    change = prediction["predictedDemandChange"]
    if prediction.get("insufficientHistory"):
        summary = (
            f"직전 {days}일 발주 데이터가 부족해 증감률을 산출하지 않았습니다. "
            f"최근 {days}일 일평균 발주량은 {prediction['recentDaily']:,.1f}입니다."
        )
    else:
        direction = "증가" if change > 0 else "감소" if change < 0 else "보합"
        summary = (
            f"합성 발주 이력 기준 최근 {days}일 수요가 직전 대비 "
            f"{abs(change) * 100:.1f}% {direction}했습니다."
        )

    return {
        "demandChange": change,
        "confidence": settings.mock_confidence,
        "summary": summary,
        "factors": factors,
        "basis": prediction["basis"],
    }


def get_forecast(
    material_code: str,
    materials: Sequence[dict],
    drug: str,
    category: str,
    days: int,
    as_of: date,
) -> dict:
    """FORECASTER 설정에 따라 예측을 돌려준다. LLM 실패 시 합성으로 폴백한다."""
    if settings.forecaster == "llm":
        result = llm_forecaster.forecast(drug, category, as_of, days)
        if result is not None:
            # MOCK_CONFIDENCE를 켜면 LLM이 준 값보다 우선한다 (데모용 고정)
            if settings.mock_confidence is not None:
                result = {**result, "confidence": settings.mock_confidence}
            return result
        logger.info("[Forecast] LLM 실패 → SYNTHETIC_TREND 폴백")

    return _synthetic(material_code, materials, days, as_of)
