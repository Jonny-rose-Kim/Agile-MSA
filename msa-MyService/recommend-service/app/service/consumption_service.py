"""
재고 소진 시뮬레이션.

평상시 일 소진량에 예측 증감률을 곱해 days일간 재고를 깎아보고,
임계치 하회일 / 소진일 / 기말 재고 / 부족량 / 권장 발주량을 낸다.
"""

import logging
import math
from datetime import date, timedelta
from typing import Optional, Sequence

import pandas as pd

from app.config.settings import settings
from app.data import synthetic_generator as synth

logger = logging.getLogger(__name__)

BASELINE_WINDOW_DAYS = 90


def baseline_daily_consumption(
    material_code: str,
    materials: Sequence[dict],
    as_of: date,
    real_orders: Optional[Sequence[dict]] = None,
) -> float:
    """
    평상시 일 소진량.

    order-service 실이력이 있으면 최근 90일 발주량 / 90,
    없으면 합성 demand_history()의 최근 90일 평균을 쓴다.
    """
    if real_orders:
        window_start = as_of - timedelta(days=BASELINE_WINDOW_DAYS)
        total = sum(
            o.get("quantity", 0)
            for o in real_orders
            if o.get("materialCode") == material_code
            and window_start < _as_date(o.get("orderedAt")) <= as_of
        )
        logger.info(f"[Consumption] 실이력 기준 - {material_code}, 90일 합 {total}")
        return total / BASELINE_WINDOW_DAYS

    history = synth.demand_history(materials)
    series = (
        history[history["materialCode"] == material_code]
        .set_index("date")["orderQty"]
        .sort_index()
    )
    if series.empty:
        return 0.0

    end = pd.Timestamp(as_of)
    window = series.loc[(series.index > end - pd.Timedelta(days=BASELINE_WINDOW_DAYS))
                        & (series.index <= end)]
    if window.empty:
        return 0.0

    return float(window.sum()) / BASELINE_WINDOW_DAYS


def _as_date(value) -> date:
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def simulate(
    quantity: float,
    threshold: Optional[float],
    baseline_daily: float,
    demand_change: float,
    days: int,
    as_of: date,
) -> dict:
    """
    days일간 하루 단위로 재고를 깎는다.

    반환: dailyConsumption / shortageDate / stockoutDate / projectedQuantity
          / shortfall / recommendedOrderQty / recommendedThreshold
    """
    predicted_daily = baseline_daily * (1 + demand_change)

    shortage_date: Optional[date] = None
    stockout_date: Optional[date] = None
    stock = quantity

    for day in range(1, days + 1):
        stock -= predicted_daily
        current = as_of + timedelta(days=day)

        if shortage_date is None and threshold is not None and stock < threshold:
            shortage_date = current
        if stockout_date is None and stock <= 0:
            stockout_date = current
            break                      # 재고가 0이면 그 뒤는 볼 필요가 없다

    projected = max(0.0, quantity - predicted_daily * days)

    # 기간 내 총 소진량을 감당하고도 임계치가 남아야 한다.
    needed = (threshold or 0) + predicted_daily * days
    shortfall = max(0.0, needed - quantity)

    return {
        "dailyConsumption": {
            "baseline": round(baseline_daily, 2),
            "predicted": round(predicted_daily, 2),
        },
        "shortageDate": shortage_date,
        "stockoutDate": stockout_date,
        "projectedQuantity": round(projected, 1),
        "shortfall": round(shortfall, 1),
        # 발주는 쪼갤 수 없으니 올림한다 (원료 unit 기준)
        "recommendedOrderQty": math.ceil(shortfall),
        "recommendedThreshold": round(predicted_daily * settings.lead_time_days, 1),
    }
