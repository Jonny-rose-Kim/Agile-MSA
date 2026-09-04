"""
GET /api/recommend 오케스트레이션.

  재고 조회 → 수요 예측 → 소진 시뮬레이션 → 공급사 추천

각 단계는 이미 개별 모듈에 있고, 여기서는 순서와 값 전달만 맡는다.
어느 단계가 비어도(재고 미등록, 공급사 0건) 나머지는 정상 응답한다 —
추천은 비핵심 기능이라 화면 전체가 실패하면 안 된다.
"""

import logging
from datetime import date
from typing import Optional

from app.client.external_client import external_client
from app.client.inventory_client import inventory_client
from app.client.material_client import material_client
from app.config.settings import settings
from app.data import drug_material_map
from app.service import consumption_service, supplier_service
from app.service.forecast import get_forecast

logger = logging.getLogger(__name__)


class RecommendService:

    async def get_recommendation(
        self,
        material_code: str,
        quantity: Optional[float] = None,
        days: Optional[int] = None,
        as_of: Optional[date] = None,
        bearer_token: Optional[str] = None,
    ) -> dict:
        days = days or settings.forecast_default_days
        resolved_as_of = external_client.as_of(as_of).date()

        # --- 1. 원료 · 재고 ---
        materials = await material_client.list_materials()
        material = await material_client.get_material(material_code) or {}
        inventory = await inventory_client.get_my_inventory(material_code, bearer_token) or {}

        drug = drug_material_map.lookup(material_code, material.get("category"))

        # --- 2. 수요 예측 ---
        forecast = get_forecast(
            material_code=material_code,
            materials=materials,
            drug=drug["drug"],
            category=drug["category"],
            days=days,
            as_of=resolved_as_of,
        )

        # --- 3. 소진 시뮬레이션 ---
        baseline_daily = consumption_service.baseline_daily_consumption(
            material_code, materials, resolved_as_of
        )
        simulation = consumption_service.simulate(
            quantity=float(inventory.get("quantity") or 0),
            threshold=_as_float(inventory.get("threshold")),
            baseline_daily=baseline_daily,
            demand_change=forecast["demandChange"],
            days=days,
            as_of=resolved_as_of,
        )

        # quantity 를 안 넘기면 권장 발주량을 필요 수량으로 쓴다.
        need_quantity = quantity if quantity else simulation["recommendedOrderQty"]

        # --- 4. 공급사 추천 ---
        candidates = await material_client.get_candidates(material_code)
        suppliers = supplier_service.select_suppliers(candidates, need_quantity)

        logger.info(
            f"[Recommend] {material_code} - 기준일 {resolved_as_of}, {days}일, "
            f"증감률 {forecast['demandChange']:+.1%}, 필요수량 {need_quantity:,.0f}, "
            f"공급사 {len(suppliers)}곳"
        )

        return {
            "material": {
                "materialId": material.get("materialId"),
                "materialCode": material_code,
                "name": material.get("name"),
                "category": material.get("category"),
                "unit": material.get("unit"),
                "drug": drug["drug"],
            },
            "inventory": {
                "inventoryId": inventory.get("inventoryId"),
                "quantity": _as_float(inventory.get("quantity")),
                "threshold": _as_float(inventory.get("threshold")),
            },
            "forecast": {
                "days": days,
                "asOf": resolved_as_of,
                "basis": forecast["basis"],
                "demandChange": forecast["demandChange"],
                "confidence": forecast.get("confidence"),
                "summary": forecast.get("summary", ""),
                "factors": forecast.get("factors", []),
                **simulation,
            },
            "suppliers": suppliers,
            "needQuantity": need_quantity,
            "mock": settings.mock_mode,
        }


def _as_float(value) -> Optional[float]:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


recommend_service = RecommendService()
