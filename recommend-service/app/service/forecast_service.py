"""
수요 예측 — 구 GET /api/forecast/demand + /api/forecast/factors 에 해당하는 내부 함수.

ponytail: 회귀/시계열 모델이 아니라 합성 수요 이력의 구간 합 비교다.
천장: 추세의 방향과 크기만 맞고, 외부 지표를 예측에 직접 쓰지 않는다.
교체 경로: demand_history가 실주문 이력으로 바뀌면 이 모듈의
          predict_demand()만 회귀 모델로 교체한다. basis 값으로 구분한다.
"""

import logging
from datetime import date
from typing import List, Optional, Sequence

import pandas as pd

from app.client.external_client import external_client
from app.config.settings import settings
from app.data import synthetic_generator as synth

logger = logging.getLogger(__name__)

BASIS_SYNTHETIC = "SYNTHETIC_TREND"


def get_factors(today: date | None = None) -> List[dict]:
    """구 GET /api/forecast/factors."""
    return external_client.get_factors(today)


def predict_demand(
    material_code: str,
    materials: Sequence[dict],
    days: int,
    today: date | None = None,
) -> dict:
    """
    구 GET /api/forecast/demand.

    최근 days 구간의 발주 합 vs 그 직전 days 구간의 발주 합으로 증감률을 낸다.
    반환: {predictedDemand, predictedDemandChange, dailyAverage, confidence, basis, reason}
    """
    history = synth.demand_history(materials)
    series = (
        history[history["materialCode"] == material_code]
        .set_index("date")["orderQty"]
        .sort_index()
    )

    if series.empty:
        logger.warning(f"[Forecast] 수요 이력 없음 - materialCode: {material_code}")
        return {
            "predictedDemand": 0.0,
            "predictedDemandChange": 0.0,
            "dailyAverage": 0.0,
            "confidence": settings.mock_confidence,
            "basis": BASIS_SYNTHETIC,
            "recentDaily": 0.0,
            "insufficientHistory": True,
        }

    end = external_client.as_of(today)

    # 데이터가 있는 구간. 창이 이 밖으로 나가면 그만큼만 덮인다.
    data_start, data_end = series.index.min(), series.index.max()

    def window(offset: int) -> tuple[float, int]:
        """(구간 발주 합, 실제 데이터가 덮인 일수)를 돌려준다."""
        hi = end - pd.Timedelta(days=offset * days)
        lo = hi - pd.Timedelta(days=days)
        chunk = series.loc[(series.index > lo) & (series.index <= hi)]
        covered = (min(hi, data_end) - max(lo, data_start - pd.Timedelta(days=1))).days
        return float(chunk.sum()), max(0, covered)

    recent_sum, recent_covered = window(0)
    previous_sum, previous_covered = window(1)

    # 원시 합을 그대로 비교하면 안 된다.
    # 합성 데이터 시작(2025-07-14) 근처에서는 직전 창이 며칠만 덮이는데,
    # 그러면 "8일 합 vs 90일 합"이 되어 증감률이 +900% 같은 허수로 튄다.
    # 덮인 일수로 나눈 일평균끼리 비교한다.
    recent_daily = recent_sum / recent_covered if recent_covered else 0.0
    previous_daily = previous_sum / previous_covered if previous_covered else 0.0

    # 직전 창이 절반도 안 덮이면 추세를 말할 수 없다.
    MIN_COVERAGE = 0.5
    insufficient = previous_covered < days * MIN_COVERAGE

    if insufficient or previous_daily <= 0:
        change = 0.0
    else:
        change = (recent_daily - previous_daily) / previous_daily

    predicted = recent_daily * days * (1 + change)
    daily_average = predicted / days if days else 0.0

    return {
        "predictedDemand": round(predicted, 1),
        "predictedDemandChange": round(change, 4),
        "dailyAverage": daily_average,
        # 합성 데이터 구간에서는 신뢰도를 계산할 근거가 없다.
        # MOCK_CONFIDENCE를 켜면 화면에 "-"가 남지 않도록 고정값을 내려준다.
        "confidence": settings.mock_confidence,
        "basis": BASIS_SYNTHETIC,
        "recentDaily": recent_daily,
        "previousDaily": previous_daily,
        "previousCovered": previous_covered,
        "insufficientHistory": insufficient,
    }


def recommended_threshold(
    current_threshold: Optional[float],
    predicted_change: float,
    daily_average: float,
) -> Optional[float]:
    """
    권장 재고 임계치.

    - 기존 임계치가 있으면: threshold × (1 + 증감률) × 리드타임 계수
    - 없으면:               예측 일평균 × 리드타임 일수

    리드타임 계수는 lead_time_days / 30 이다. (기본 30일 → 계수 1.0)
    """
    lead_days = settings.lead_time_days

    if current_threshold is not None:
        coefficient = lead_days / 30.0
        return round(current_threshold * (1 + predicted_change) * coefficient)

    if daily_average > 0:
        return round(daily_average * lead_days)

    return None


# 지표 영향도 우선순위. 같은 impact면 앞에 있는 지표를 고른다.
_IMPACT_RANK = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}
_TREND_LABEL = {"RISING": "상승 중", "FALLING": "하락 중", "FLAT": "보합"}


def dominant_factor(factors: List[dict]) -> Optional[dict]:
    """영향도가 가장 크고 방향이 뚜렷한 지표 하나를 고른다."""
    if not factors:
        return None
    return min(
        factors,
        key=lambda f: (
            _IMPACT_RANK.get(f["impact"], 3),
            0 if f["trend"] != "FLAT" else 1,   # 방향이 있는 쪽을 먼저
        ),
    )


def compose_reason(
    prediction: dict,
    factors: List[dict],
    days: int,
    current_threshold: Optional[float],
    recommended_threshold: Optional[float],
) -> str:
    """
    "지표 상황 + 발주 증감 + 임계치 권고"를 한 문장으로 합친다.

    예) 인플루엔자 의사환자분율이 유행기준(9.1)을 넘어 상승 중이며,
        최근 90일 발주량이 직전 대비 34.2% 증가했습니다.
        리드타임을 고려해 임계치 50 → 120 상향을 권장합니다.
    """
    sentences = []

    factor = dominant_factor(factors)
    if factor:
        latest = factor["recent"][-1]["value"] if factor["recent"] else None
        trend = _TREND_LABEL.get(factor["trend"], factor["trend"])
        if factor["name"].startswith("인플루엔자") and latest is not None:
            threshold_note = (
                f"유행기준({synth.ILI_EPIDEMIC_THRESHOLD})을 "
                f"{'넘어' if latest > synth.ILI_EPIDEMIC_THRESHOLD else '밑돌며'} "
            )
            sentences.append(f"{factor['name']}이 {threshold_note}{trend}이며")
        else:
            sentences.append(f"{factor['name']}이 {trend}이며")

    change = prediction["predictedDemandChange"]
    if prediction.get("insufficientHistory"):
        demand_part = (
            f"최근 {days}일 발주량은 일평균 {prediction['recentDaily']:,.1f}입니다"
            f"(직전 {days}일 데이터가 부족해 증감률은 산출하지 않았습니다)"
        )
    else:
        direction = "증가" if change > 0 else "감소" if change < 0 else "보합"
        demand_part = (
            f"최근 {days}일 발주량이 직전 대비 {abs(change) * 100:.1f}% {direction}했습니다"
        )

    sentences.append(demand_part)
    text = ", ".join(sentences) if len(sentences) > 1 else sentences[0]
    if not text.endswith("다"):
        text += "."
    else:
        text += "."

    # 임계치 권고
    if recommended_threshold is not None:
        if current_threshold is None:
            text += f" 리드타임을 고려해 임계치 {recommended_threshold:,.0f} 설정을 권장합니다."
        elif round(recommended_threshold) != round(current_threshold):
            move = "상향" if recommended_threshold > current_threshold else "하향"
            text += (
                f" 리드타임을 고려해 임계치 {current_threshold:,.0f} → "
                f"{recommended_threshold:,.0f} {move}을 권장합니다."
            )
        else:
            text += f" 현재 임계치 {current_threshold:,.0f} 유지를 권장합니다."

    return text
