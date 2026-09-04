"""GET /api/recommend 응답 모델."""

from datetime import date
from typing import List, Optional

from pydantic import BaseModel, Field


class Factor(BaseModel):
    """수요에 영향을 준 외부 지표 한 건."""
    name: str
    trend: str = Field(description="RISING | FALLING | FLAT")
    impact: str = Field(description="HIGH | MEDIUM | LOW")
    evidence: str = ""
    source_url: str = ""


class DailyConsumption(BaseModel):
    baseline: float = Field(description="평상시 일 소진량")
    predicted: float = Field(description="예측 증감률을 반영한 일 소진량")


class Forecast(BaseModel):
    days: int
    asOf: date
    basis: str = Field(description="LLM_FORECAST | SYNTHETIC_TREND")
    demandChange: float = Field(description="평상시 대비 수요 변화율")
    confidence: Optional[float] = None
    summary: str = ""
    dailyConsumption: DailyConsumption
    shortageDate: Optional[date] = Field(None, description="임계치를 밑도는 날")
    stockoutDate: Optional[date] = Field(None, description="재고가 0이 되는 날")
    projectedQuantity: float = Field(description="예측 기간 말 잔여 재고")
    shortfall: float = Field(description="기간 내 부족량")
    recommendedOrderQty: float
    recommendedThreshold: Optional[float] = None
    factors: List[Factor] = []


class Material(BaseModel):
    materialId: Optional[int] = None
    materialCode: str
    name: Optional[str] = None
    category: Optional[str] = None
    unit: Optional[str] = None
    drug: Optional[str] = Field(None, description="이 원료로 만드는 대표 완제의약품")


class Inventory(BaseModel):
    inventoryId: Optional[int] = None
    quantity: Optional[float] = None
    threshold: Optional[float] = None


class Supplier(BaseModel):
    materialId: Optional[int] = None
    supplierId: Optional[int] = None
    supplierName: Optional[str] = None
    gmpCertified: Optional[bool] = None
    price: Optional[float] = None
    availableCapacity: Optional[float] = None
    originCountry: Optional[str] = None
    pastOrderCount: Optional[int] = None
    estimatedCost: Optional[float] = None


class RecommendResponse(BaseModel):
    material: Material
    inventory: Inventory
    forecast: Forecast
    suppliers: List[Supplier] = []
    needQuantity: Optional[float] = Field(
        None, description="공급사 필터에 쓴 필요 수량. quantity 를 생략하면 권장 발주량이 들어간다."
    )
    mock: bool = Field(description="true 면 원료·재고·공급사가 데모용 고정값이다")
