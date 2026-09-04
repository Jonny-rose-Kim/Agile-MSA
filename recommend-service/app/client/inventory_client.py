import logging
from typing import Optional

import httpx

from app.config.settings import settings
from app.data import mock_catalog

logger = logging.getLogger(__name__)


class InventoryClient:
    """
    order-service 클라이언트 — 로그인 유저의 보유 재고 조회.

    GET /api/inventories/my 응답을 materialCode로 필터한다.
    응답에 materialCode가 없으면 필터가 불가능하다 (팀 요청 목록 참고).
    """

    def __init__(self):
        self.base_url = settings.order_service_url.rstrip("/")

    async def get_my_inventory(self, material_code: str, bearer_token: str | None = None) -> Optional[dict]:
        if settings.mock_mode:
            return mock_catalog.MOCK_INVENTORIES.get(material_code)

        url = f"{self.base_url}/api/inventories/my"
        headers = {"Authorization": bearer_token} if bearer_token else {}
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url, headers=headers)
                response.raise_for_status()
                body = response.json()
        except httpx.HTTPError as e:
            logger.warning(f"[InventoryClient] 재고 조회 실패 - url: {url}, error: {e}")
            return None

        rows = body.get("data", body) if isinstance(body, dict) else body
        if not isinstance(rows, list):
            return None

        return next((r for r in rows if r.get("materialCode") == material_code), None)


inventory_client = InventoryClient()
