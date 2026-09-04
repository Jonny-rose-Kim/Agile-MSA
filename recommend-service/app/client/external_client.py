"""
외부 지표 클라이언트 (질병청 ILI / 기상청 기온).

현재는 합성 생성기를 읽는 mock 구현뿐이다.
실 API 수집기로 교체할 때 get_factors()의 반환 형태만 유지하면 상위는 그대로다.
"""

import logging
from datetime import date
from typing import List

import pandas as pd

from app.config.settings import settings
from app.data import synthetic_generator as synth

logger = logging.getLogger(__name__)

# 지표 메타. name/source는 응답에 그대로 실린다.
FACTOR_META = {
    "ili": ("인플루엔자 의사환자분율", "질병관리청 (합성)"),
    "temp": ("서울 일평균기온", "기상청 (합성)"),
    "resp_index": ("호흡기 질환 지수", "합성 지표"),
}


def _trend(values: pd.Series) -> str:
    """최근 절반 평균과 이전 절반 평균을 비교해 방향을 잡는다."""
    if len(values) < 4:
        return "FLAT"
    half = len(values) // 2
    before, after = values.iloc[:half].mean(), values.iloc[half:].mean()
    if before == 0:
        return "FLAT"
    change = (after - before) / abs(before)
    if change > 0.10:
        return "RISING"
    if change < -0.10:
        return "FALLING"
    return "FLAT"


def _impact(column: str, values: pd.Series) -> str:
    """ILI는 유행기준 대비로, 나머지는 변동폭으로 영향도를 잡는다."""
    if column == "ili":
        peak = values.max()
        if peak >= synth.ILI_EPIDEMIC_THRESHOLD * 2:
            return "HIGH"
        return "MEDIUM" if peak >= synth.ILI_EPIDEMIC_THRESHOLD else "LOW"
    spread = values.max() - values.min()
    if column == "temp":
        return "HIGH" if spread >= 15 else "MEDIUM" if spread >= 7 else "LOW"
    return "HIGH" if spread >= 20 else "MEDIUM" if spread >= 8 else "LOW"


class ExternalFactorClient:
    def as_of(self, today: date | None = None) -> pd.Timestamp:
        """
        기준일을 하나로 결정한다. 우선순위: 인자 asOf > DEMO_AS_OF > 오늘.

        합성 데이터 구간을 벗어나면 양끝으로 당긴다.
        (오늘이 데이터 끝보다 뒤면 최근 12주가 통째로 비기 때문)
        """
        resolved = today or settings.demo_as_of or date.today()
        clamped = min(max(pd.Timestamp(resolved), pd.Timestamp(synth.START_DATE)),
                      pd.Timestamp(synth.END_DATE))
        if clamped.date() != resolved:
            logger.warning(
                f"[ExternalClient] 기준일 {resolved}이 데이터 구간 밖이라 {clamped.date()}로 조정"
            )
        return clamped

    def get_factors(self, today: date | None = None) -> List[dict]:
        """
        요청일 기준 최근 N주(기본 12주)를 주 단위로 리샘플해 반환한다.
        반환: [{name, source, trend, impact, recent: [{date, value}]}]
        """
        df = synth.external_factors().set_index("date")
        end = self.as_of(today)
        start = end - pd.Timedelta(weeks=settings.factor_recent_weeks)
        window = df.loc[(df.index > start) & (df.index <= end)]

        factors: List[dict] = []
        for column, (name, source) in FACTOR_META.items():
            if column not in window.columns:
                continue
            # 일 단위 → 주 단위(주 시작 월요일 기준 평균)
            weekly = window[column].resample("W-MON", label="left", closed="left").mean().dropna()
            weekly = weekly.round(2)

            factors.append(
                {
                    "name": name,
                    "source": source,
                    "trend": _trend(weekly),
                    "impact": _impact(column, weekly),
                    "recent": [
                        {"date": ts.date(), "value": float(v)} for ts, v in weekly.items()
                    ],
                }
            )

        logger.info(f"[ExternalClient] factors {len(factors)}건 - 기준일 {end.date()}")
        return factors


external_client = ExternalFactorClient()
