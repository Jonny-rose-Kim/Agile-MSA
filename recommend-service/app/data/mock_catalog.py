"""
material-service / order-service 미구현 구간을 메우는 mock 데이터.

여기 값은 전부 **실제 조회 결과가 아니라 데모용 고정값**이다.
응답의 forecast.basis 가 SYNTHETIC_TREND 인 동안은 이 표가 근거다.
MOCK_MODE=false 로 두면 실제 서비스를 호출하고 이 모듈은 쓰이지 않는다.

카테고리·원산지 값은 material-service enum 확정 전 예상값이다.
(팀 요청 목록 - enum 확정되면 이 표만 맞추면 된다)
"""

from datetime import date, timedelta
from typing import Dict, List, Optional

# --- 원료 카탈로그 (material-service GET /api/materials 대응)
MOCK_MATERIALS: List[dict] = [
    {"materialId": 1, "materialCode": "API-CEFA-500", "name": "세파클러 원료",
     "category": "ANTIBIOTIC", "unit": "kg"},
    {"materialId": 2, "materialCode": "API-ACET-325", "name": "아세트아미노펜 원료",
     "category": "ANALGESIC_ANTIPYRETIC", "unit": "kg"},
    {"materialId": 3, "materialCode": "API-OSEL-075", "name": "오셀타미비르 원료",
     "category": "ANTIVIRAL", "unit": "g"},
    {"materialId": 4, "materialCode": "API-LORA-010", "name": "로라타딘 원료",
     "category": "ANTIHISTAMINE", "unit": "kg"},
    {"materialId": 5, "materialCode": "API-AMBR-030", "name": "암브록솔 원료",
     "category": "RESPIRATORY", "unit": "kg"},
    # CATEGORY_RULES에 없는 카테고리 → DEFAULT_RULE(노이즈만)로 떨어지는 경로 확인용
    {"materialId": 6, "materialCode": "API-DEXA-004", "name": "덱사메타손 원료",
     "category": "CORTICOSTEROID", "unit": "g"},
]

# --- 보유 재고 (order-service GET /api/inventories/my 대응). materialCode로 조회한다.
# 원료 6종 전부 등록해서 화면에 "-"가 남지 않게 한다.
#
# 수치는 소진 시뮬레이션 기준으로 잡았다 (합성 일 소진량 대비):
#   threshold ≈ 30일치 (조달 리드타임)
#   quantity  ≈ 70~75일치 → 90일 예측 구간 안에서 임계치 하회일이 잡힌다
MOCK_INVENTORIES: Dict[str, dict] = {
    # 일 소진 약 31.8kg
    "API-CEFA-500": {"inventoryId": 101, "materialCode": "API-CEFA-500", "quantity": 2400, "threshold": 1000},
    # 일 소진 약 32.4kg. 수요가 늘면 임계치 하회일이 앞당겨지는 것을 보여주는 데모 대상.
    "API-ACET-325": {"inventoryId": 102, "materialCode": "API-ACET-325", "quantity": 2400, "threshold": 1000},
    # 일 소진 약 24,400g
    "API-OSEL-075": {"inventoryId": 103, "materialCode": "API-OSEL-075", "quantity": 1800000, "threshold": 750000},
    # 일 소진 약 19.1kg
    "API-LORA-010": {"inventoryId": 104, "materialCode": "API-LORA-010", "quantity": 1400, "threshold": 600},
    # 일 소진 약 22.3kg
    "API-AMBR-030": {"inventoryId": 105, "materialCode": "API-AMBR-030", "quantity": 1600, "threshold": 700},
    # 일 소진 약 25,800g
    "API-DEXA-004": {"inventoryId": 106, "materialCode": "API-DEXA-004", "quantity": 1900000, "threshold": 780000},
}

# --- 후보 공장 (material-service GET /api/materials/internal/candidates 대응)
# 원료마다 2~3곳. 원산지·GMP를 섞어서 리스크 등급이 갈리게 구성했다.
#   국내 인증(KR+GMP) / 인도 인증(IN+GMP) / 중국 미인증(CN+GMP 없음)
MOCK_SUPPLIERS: Dict[str, List[dict]] = {
    "API-CEFA-500": [
        {"materialId": 1, "supplierId": 7, "supplierName": "한국API공장",
         "price": 42000, "availableCapacity": 5000, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 1, "supplierId": 12, "supplierName": "대성파인케미칼",
         "price": 38500, "availableCapacity": 1200, "gmpCertified": True, "originCountry": "IN"},
        {"materialId": 1, "supplierId": 21, "supplierName": "신흥원료합성",
         "price": 35000, "availableCapacity": 600, "gmpCertified": False, "originCountry": "CN"},
    ],
    "API-ACET-325": [
        {"materialId": 2, "supplierId": 7, "supplierName": "한국API공장",
         "price": 18000, "availableCapacity": 9000, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 2, "supplierId": 33, "supplierName": "동방정밀화학",
         "price": 17200, "availableCapacity": 3000, "gmpCertified": True, "originCountry": "IN"},
        {"materialId": 2, "supplierId": 21, "supplierName": "신흥원료합성",
         "price": 14800, "availableCapacity": 900, "gmpCertified": False, "originCountry": "CN"},
    ],
    "API-OSEL-075": [
        {"materialId": 3, "supplierId": 41, "supplierName": "바이오젠원료",
         "price": 96000, "availableCapacity": 800, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 3, "supplierId": 52, "supplierName": "라이프사이언스인디아",
         "price": 81000, "availableCapacity": 2400, "gmpCertified": True, "originCountry": "IN"},
    ],
    "API-LORA-010": [
        {"materialId": 4, "supplierId": 12, "supplierName": "대성파인케미칼",
         "price": 22000, "availableCapacity": 4000, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 4, "supplierId": 21, "supplierName": "신흥원료합성",
         "price": 19500, "availableCapacity": 2200, "gmpCertified": False, "originCountry": "CN"},
    ],
    "API-AMBR-030": [
        {"materialId": 5, "supplierId": 33, "supplierName": "동방정밀화학",
         "price": 27000, "availableCapacity": 2600, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 5, "supplierId": 52, "supplierName": "라이프사이언스인디아",
         "price": 23400, "availableCapacity": 1500, "gmpCertified": True, "originCountry": "IN"},
        {"materialId": 5, "supplierId": 66, "supplierName": "웨이하이케미칼",
         "price": 20800, "availableCapacity": 700, "gmpCertified": False, "originCountry": "CN"},
    ],
    "API-DEXA-004": [
        {"materialId": 6, "supplierId": 41, "supplierName": "바이오젠원료",
         "price": 54000, "availableCapacity": 40000, "gmpCertified": True, "originCountry": "KR"},
        {"materialId": 6, "supplierId": 66, "supplierName": "웨이하이케미칼",
         "price": 45000, "availableCapacity": 12000, "gmpCertified": False, "originCountry": "CN"},
    ],
}

# --- 과거 거래 이력 (order-service 확정 주문 대응)
# (원료코드, 공급자ID) → 거래 횟수. pastOrderCount는 이 표에서 센다.
# 신흥원료합성(21)은 신규 거래처라 0건인 케이스를 일부러 남겨뒀다.
PAST_ORDER_COUNTS: Dict[tuple, int] = {
    ("API-CEFA-500", 7): 34,
    ("API-CEFA-500", 12): 11,
    ("API-CEFA-500", 21): 0,
    ("API-ACET-325", 7): 52,
    ("API-ACET-325", 33): 20,
    ("API-ACET-325", 21): 2,
    ("API-OSEL-075", 41): 7,
    ("API-OSEL-075", 52): 13,
    ("API-LORA-010", 12): 15,
    ("API-LORA-010", 21): 0,
    ("API-AMBR-030", 33): 9,
    ("API-AMBR-030", 52): 6,
    ("API-AMBR-030", 66): 3,
    ("API-DEXA-004", 41): 22,
    ("API-DEXA-004", 66): 1,
}

# 과거 거래는 모두 이 날짜 이전으로 깐다. (데모 기준일 asOf=2025-10-20 보다 앞)
LAST_ORDER_DATE = date(2025, 10, 10)


def _build_orders() -> List[dict]:
    """PAST_ORDER_COUNTS를 주문 레코드로 펼친다. 14일 간격으로 과거로 거슬러 깐다."""
    orders: List[dict] = []
    order_id = 9000
    for (material_code, supplier_id), count in PAST_ORDER_COUNTS.items():
        for n in range(count):
            order_id += 1
            orders.append(
                {
                    "orderId": order_id,
                    "materialCode": material_code,
                    "supplierId": supplier_id,
                    "orderedAt": LAST_ORDER_DATE - timedelta(days=14 * n),
                    "quantity": 100 + (order_id % 7) * 50,
                }
            )
    return orders


MOCK_ORDERS: List[dict] = _build_orders()   # 195건


def past_order_count(material_code: str, supplier_id: int) -> int:
    return PAST_ORDER_COUNTS.get((material_code, supplier_id), 0)


def find_material(material_code: str) -> Optional[dict]:
    return next((m for m in MOCK_MATERIALS if m["materialCode"] == material_code), None)
