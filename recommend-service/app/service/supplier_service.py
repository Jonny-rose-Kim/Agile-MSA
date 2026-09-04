"""공급사 추천 — 구 GET /api/recommend/suppliers 에 해당하는 내부 함수."""

import logging
from typing import List, Optional

from app.config.settings import settings

logger = logging.getLogger(__name__)


def select_suppliers(candidates: List[dict], need_quantity: Optional[float]) -> List[dict]:
    """
    필터: GMP 인증 보유 AND availableCapacity >= 필요 수량
    정렬: 과거 거래 횟수 내림차순 → 단가 오름차순
    estimatedCost = price × 필요 수량
    """
    filtered = [c for c in candidates if c.get("gmpCertified")]

    if need_quantity:
        filtered = [
            c for c in filtered
            if c.get("availableCapacity") is None or c["availableCapacity"] >= need_quantity
        ]

    logger.info(
        f"[Supplier] GMP·수량({need_quantity}) 필터 - {len(candidates)}건 → {len(filtered)}건"
    )

    filtered.sort(
        key=lambda c: (
            -(c.get("pastOrderCount") or 0),
            c.get("price") if c.get("price") is not None else float("inf"),
        )
    )

    result = []
    for c in filtered[: settings.recommend_max_count]:
        row = dict(c)
        price = c.get("price")
        row["estimatedCost"] = (
            round(price * need_quantity, 1) if price is not None and need_quantity else None
        )
        result.append(row)
    return result
