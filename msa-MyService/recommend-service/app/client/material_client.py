import logging
from typing import List, Optional

import httpx

from app.config.settings import settings
from app.data import mock_catalog

logger = logging.getLogger(__name__)


class MaterialServiceClient:
    """
    material-service 클라이언트 — 원료 정보 / 후보 공장 조회.

    MOCK_MODE=true 이면 app/data/mock_catalog.py를 쓴다.
    실서비스 호출이 실패해도 예외를 던지지 않는다. 추천은 비핵심 기능이라
    공장 목록이 비어도 수요 예측은 정상 응답해야 한다.
    """

    def __init__(self):
        self.base_url = settings.material_service_url.rstrip("/")

    async def _get(self, path: str, params: dict) -> Optional[object]:
        url = f"{self.base_url}{path}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                body = response.json()
        except httpx.HTTPError as e:
            logger.warning(f"[MaterialClient] 호출 실패 - url: {url}, error: {e}")
            return None
        # 래퍼({success,message,data})로 오든 그대로 오든 둘 다 받는다.
        return body.get("data", body) if isinstance(body, dict) else body

    async def get_material(self, material_code: str) -> Optional[dict]:
        if settings.mock_mode:
            return mock_catalog.find_material(material_code)

        data = await self._get(f"/api/materials/code/{material_code}", {})
        return data if isinstance(data, dict) else None

    async def get_candidates(self, material_code: str) -> List[dict]:
        """GET /api/materials/internal/candidates — 추천 후보 공장."""
        if settings.mock_mode:
            # pastOrderCount는 별도 표(과거 주문 이력)에서 센다.
            return [
                {**c, "pastOrderCount": mock_catalog.past_order_count(material_code, c["supplierId"])}
                for c in mock_catalog.MOCK_SUPPLIERS.get(material_code, [])
            ]

        data = await self._get("/api/materials/internal/candidates", {"materialCode": material_code})
        return data if isinstance(data, list) else []

    async def list_materials(self) -> List[dict]:
        """수요 이력 생성을 위한 원료 카탈로그."""
        if settings.mock_mode:
            return list(mock_catalog.MOCK_MATERIALS)

        data = await self._get("/api/materials", {})
        return data if isinstance(data, list) else []


material_client = MaterialServiceClient()
