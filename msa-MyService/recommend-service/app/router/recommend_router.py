import logging
from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, Header, Query

from app.config.security import verify_token
from app.model.schemas import RecommendResponse
from app.service.recommend_service import recommend_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/recommend", tags=["recommend"])


@router.get("", response_model=RecommendResponse)
async def get_recommendation(
    materialCode: str = Query(..., description="원료코드"),
    quantity: Optional[float] = Query(None, description="필요 수량. 생략하면 권장 발주량을 쓴다"),
    days: Optional[int] = Query(None, ge=1, le=365, description="예측 기간(일). 기본 90"),
    horizon: Optional[int] = Query(None, ge=1, le=365, description="days 의 별칭(화면 호환)"),
    asOf: Optional[date] = Query(None, description="기준일 YYYY-MM-DD. 데모용"),
    authorization: Optional[str] = Header(None),
    token_payload: dict = Depends(verify_token),
):
    """
    원료 하나에 대해 수요 예측·소진 시뮬레이션·공급사 추천을 한 번에 돌려준다.

    재고 조회는 로그인 사용자 기준이라 Authorization 헤더를 그대로 넘긴다.
    """
    return await recommend_service.get_recommendation(
        material_code=materialCode,
        quantity=quantity,
        days=days or horizon,
        as_of=asOf,
        bearer_token=authorization,
    )


@router.get("/health", include_in_schema=False)
async def health_check():
    return {"status": "UP", "service": "recommend-service"}
