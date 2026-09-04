"""
합성 수요·외부지표 데이터 생성기.

ponytail: 실제 공공데이터(질병청 ILI, 기상청 기온)와 실주문 이력 대신 수식으로 만든 값이다.
천장: 실제 수요와 상관관계가 없다. 화면·계약·모델 자리를 검증하기 위한 값이다.
교체 경로:
  - external_factors() → 질병청/기상청 API 수집기로 교체
  - demand_history()   → order-service 확정 주문 이력 집계로 교체
두 함수 모두 시그니처(일 단위 DataFrame)를 유지하면 상위 서비스는 그대로 둔 채 교체된다.

모든 난수는 seed 42로 고정되어 있어 호출·재시작과 무관하게 같은 값이 나온다.
"""

import logging
from datetime import date
from functools import lru_cache
from typing import Dict, List, Sequence, Tuple

import numpy as np
import pandas as pd

logger = logging.getLogger(__name__)

SEED = 42

# 생성 구간: 2025-W29 월요일 ~ 2026-W33 일요일 (약 400일)
START_DATE = date(2025, 7, 14)
END_DATE = date(2026, 8, 16)

# 인플루엔자 의사환자분율(ILI) 유행기준. 질병청 공표값과 같은 의미의 메타데이터다.
ILI_EPIDEMIC_THRESHOLD = 9.1

# (ISO 연, ISO 주) → ILI 값. 주 단위 앵커를 일 단위로 선형 보간한다.
ILI_ANCHORS: List[Tuple[Tuple[int, int], float]] = [
    ((2025, 29), 3.5),
    ((2025, 40), 12.1),
    ((2025, 47), 70.9),
    ((2026, 10), 25.0),
    ((2026, 19), 6.9),
    ((2026, 33), 4.0),
]

# 서울 일평균기온 사인파: 여름(8/1 부근) 27도, 겨울 -2도
TEMP_SUMMER_PEAK = 27.0
TEMP_WINTER_LOW = -2.0
TEMP_PEAK_DAY_OF_YEAR = 213  # 8월 1일


# ---------------------------------------------------------------- 외부 지표

def _daily_index() -> pd.DatetimeIndex:
    return pd.date_range(START_DATE, END_DATE, freq="D")


@lru_cache(maxsize=1)
def external_factors() -> pd.DataFrame:
    """
    일 단위 외부 지표.

    columns: date, ili, temp, resp_index
    attrs:   ili_epidemic_threshold
    """
    rng = np.random.default_rng(SEED)
    index = _daily_index()

    # --- ILI: 주 단위 앵커 → 일 단위 선형 보간
    anchor_points = pd.Series(
        {pd.Timestamp(date.fromisocalendar(y, w, 1)): v for (y, w), v in ILI_ANCHORS}
    ).sort_index()

    # 앵커 날짜를 포함한 축에서 보간한 뒤 생성 구간만 잘라낸다.
    union = index.union(pd.DatetimeIndex(anchor_points.index))
    ili = anchor_points.reindex(union).interpolate(method="time").reindex(index)
    ili = ili + rng.normal(0.0, 0.6, len(index))   # 소량 노이즈
    ili = ili.clip(lower=0.0)

    # --- 기온: 사인파 + 노이즈
    amplitude = (TEMP_SUMMER_PEAK - TEMP_WINTER_LOW) / 2.0
    midpoint = (TEMP_SUMMER_PEAK + TEMP_WINTER_LOW) / 2.0
    doy = index.dayofyear.to_numpy()
    temp = midpoint + amplitude * np.cos(2 * np.pi * (doy - TEMP_PEAK_DAY_OF_YEAR) / 365.0)
    temp = pd.Series(temp + rng.normal(0.0, 1.8, len(index)), index=index)

    # --- 호흡기 질환 지수: 7일 기온 하락폭 + ILI 합성
    temp_drop = (temp.shift(7) - temp).clip(lower=0.0).fillna(0.0)
    resp_index = (0.6 * ili + 0.4 * temp_drop * 2.0).clip(lower=0.0)

    df = pd.DataFrame(
        {
            "date": index,
            "ili": ili.to_numpy(),
            "temp": temp.to_numpy(),
            "resp_index": resp_index.to_numpy(),
        }
    ).reset_index(drop=True)

    df.attrs["ili_epidemic_threshold"] = ILI_EPIDEMIC_THRESHOLD
    logger.info(f"[Synthetic] external_factors 생성 - {len(df)}일 ({START_DATE} ~ {END_DATE})")
    return df


def temp_drop_7d() -> pd.Series:
    """
    7일 전 대비 기온 하락폭(양수만). 호흡기·알레르기 수요 동인.

    일 기온을 그대로 빼면 일교차 노이즈가 신호를 덮어버린다(상관 0.15 수준).
    실제로도 수요를 움직이는 건 하루 기온이 아니라 지속된 한파이므로,
    7일 이동평균으로 baseline을 만든 뒤 차분한다.
    """
    temp = external_factors().set_index("date")["temp"]
    smooth = temp.rolling(7, min_periods=1).mean()
    return (smooth.shift(7) - smooth).clip(lower=0.0).fillna(0.0)


# ---------------------------------------------------------------- 수요 이력

# 카테고리별 발주 규칙.
#   base        : 발주가 없는 날에도 깔리는 기본 일 수요 (kg 기준)
#   k           : 동인 지표 1단위당 추가 수요
#   driver      : "ili" | "temp_drop" | None
#   lag_days    : 동인 지표를 며칠 지연시켜 반영할지
#   driver_scale: 동인 반영 배율
#   season      : 계절 소폭 가산 진폭
#
# 키는 material-service의 category enum 값이다. 아직 확정 전이라
# 아래는 예상 값이고, 매칭되지 않는 카테고리는 DEFAULT_RULE로 떨어진다.
# (팀 요청 목록 - category enum 확정 필요)
CATEGORY_RULES: Dict[str, dict] = {
    # 해열진통제·항바이러스제: ILI 14일 지연에 강하게 반응
    "ANALGESIC_ANTIPYRETIC": dict(base=20.0, k=1.60, driver="ili", lag_days=14, driver_scale=1.0, season=0.0),
    "ANTIVIRAL": dict(base=12.0, k=1.40, driver="ili", lag_days=14, driver_scale=1.0, season=0.0),
    # 항생제(세파계 등): ILI 21일 지연 × 0.5 + 계절 소폭
    "ANTIBIOTIC": dict(base=30.0, k=1.20, driver="ili", lag_days=21, driver_scale=0.5, season=4.0),
    # 호흡기·알레르기: 7일 기온 하락폭에 반응
    "RESPIRATORY": dict(base=18.0, k=5.00, driver="temp_drop", lag_days=0, driver_scale=1.0, season=0.0),
    "ANTIHISTAMINE": dict(base=15.0, k=4.50, driver="temp_drop", lag_days=0, driver_scale=1.0, season=0.0),
}

DEFAULT_RULE = dict(base=25.0, k=0.0, driver=None, lag_days=0, driver_scale=0.0, season=0.0)

# 원료 단위별 스케일. 카테고리 상수는 kg 기준이라 g 단위 원료는 1000배.
UNIT_SCALE = {"kg": 1.0, "g": 1000.0, "mg": 1_000_000.0}

# 주당 발주일 수: 발주는 매일 나오지 않는다.
ORDER_DAYS_PER_WEEK = (2, 3)


def rule_for(category: str | None) -> dict:
    return CATEGORY_RULES.get(category or "", DEFAULT_RULE)


def _driver_series(rule: dict) -> pd.Series:
    ext = external_factors().set_index("date")
    if rule["driver"] == "ili":
        series = ext["ili"]
    elif rule["driver"] == "temp_drop":
        return temp_drop_7d().shift(rule["lag_days"]).fillna(0.0)
    else:
        return pd.Series(0.0, index=ext.index)
    return series.shift(rule["lag_days"]).fillna(0.0)


def _demand_for_material(material: dict, rng: np.random.Generator) -> pd.DataFrame:
    """원료 1건의 일 발주량. 발주일에만 양이 잡히고 나머지는 0."""
    rule = rule_for(material.get("category"))
    unit_scale = UNIT_SCALE.get(material.get("unit", "kg"), 1.0)

    index = _daily_index()
    driver = _driver_series(rule).reindex(index).fillna(0.0)

    # 잠재 일 수요 (발주가 매일 균등하게 난다고 가정했을 때의 양)
    latent = rule["base"] + rule["k"] * rule["driver_scale"] * driver.to_numpy()

    if rule["season"]:
        doy = index.dayofyear.to_numpy()
        latent = latent + rule["season"] * np.cos(2 * np.pi * (doy - 15) / 365.0)

    latent = np.clip(latent + rng.normal(0.0, 1.0, len(index)), 0.0, None)

    # 주별로 발주일을 2~3일 고르고, 그 날에 그 주의 물량을 몰아준다.
    order_qty = np.zeros(len(index))
    iso_week = pd.Series(index).dt.isocalendar().week.to_numpy()
    iso_year = pd.Series(index).dt.isocalendar().year.to_numpy()

    for key in np.unique(np.stack([iso_year, iso_week], axis=1), axis=0):
        mask = (iso_year == key[0]) & (iso_week == key[1])
        positions = np.flatnonzero(mask)
        n_orders = int(rng.integers(ORDER_DAYS_PER_WEEK[0], ORDER_DAYS_PER_WEEK[1] + 1))
        n_orders = min(n_orders, len(positions))
        chosen = rng.choice(positions, size=n_orders, replace=False)
        # 그 주 잠재 수요 총합을 발주일에 균등 배분 → 7일 롤링 합이 잠재 수요를 따라간다.
        order_qty[chosen] = latent[positions].sum() / n_orders

    order_qty = order_qty * unit_scale

    df = pd.DataFrame({"date": index, "orderQty": order_qty})
    df["materialCode"] = material["materialCode"]
    # 발주가 띄엄띄엄이라 원계열은 비교가 어렵다. 7일 롤링 합이 실질 수요다.
    df["rolling7"] = df["orderQty"].rolling(7).sum()
    return df


def demand_history(materials: Sequence[dict]) -> pd.DataFrame:
    """
    원료별 일 발주량 이력.

    materials: [{"materialCode", "category", "unit", ...}] (material-service 카탈로그 또는 mock)
    반환 columns: date, materialCode, orderQty, rolling7
    """
    return _demand_history_cached(
        tuple((m["materialCode"], m.get("category"), m.get("unit", "kg")) for m in materials)
    )


@lru_cache(maxsize=8)
def _demand_history_cached(key: Tuple[Tuple[str, str | None, str], ...]) -> pd.DataFrame:
    # 원료마다 독립 시드를 줘서, 카탈로그 순서가 바뀌어도 원료별 계열이 그대로 유지되게 한다.
    frames = []
    for code, category, unit in key:
        seed = SEED + (int.from_bytes(code.encode("utf-8"), "little") % 10_000)
        rng = np.random.default_rng(seed)
        frames.append(
            _demand_for_material(
                {"materialCode": code, "category": category, "unit": unit}, rng
            )
        )

    result = pd.concat(frames, ignore_index=True)
    logger.info(f"[Synthetic] demand_history 생성 - 원료 {len(key)}종, {len(result)}행")
    return result
