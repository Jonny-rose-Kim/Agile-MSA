"""
원료 → 대표 완제의약품 · 약효군 매핑.

LLM에게 "이 원료가 무슨 약이 되는지"를 알려주기 위한 고정 테이블이다.
원료코드로 못 찾으면 material의 category만으로 처리한다.

kg_per_1000_units: 완제 1,000단위(정/캡슐/바이알) 생산에 필요한 원료량(kg).
                   소진량을 완제 수량으로 환산할 때 쓴다.
"""

from typing import Optional

DRUG_MATERIAL_MAP = {
    "API-CEFA-500": {
        "drug": "세파클러 캡슐 500mg (세팔로스포린계 항생제)",
        "category": "항생제",
        "kg_per_1000_units": 0.500,
    },
    "API-ACET-325": {
        "drug": "아세트아미노펜 제제(해열진통제)",
        "category": "해열진통제",
        "kg_per_1000_units": 0.325,
    },
    "API-OSEL-075": {
        "drug": "오셀타미비르 캡슐 75mg (항인플루엔자 바이러스제)",
        "category": "항바이러스제",
        "kg_per_1000_units": 0.075,
    },
    "API-LORA-010": {
        "drug": "로라타딘 정 10mg (2세대 항히스타민제)",
        "category": "항히스타민제",
        "kg_per_1000_units": 0.010,
    },
    "API-AMBR-030": {
        "drug": "암브록솔 정 30mg (진해거담제)",
        "category": "호흡기용제",
        "kg_per_1000_units": 0.030,
    },
    "API-DEXA-004": {
        "drug": "덱사메타손 정 0.5mg (부신피질호르몬제)",
        "category": "스테로이드",
        "kg_per_1000_units": 0.0005,
    },
}

# material-service category enum → 약효군 한글명 (원료코드가 표에 없을 때의 폴백)
CATEGORY_FALLBACK = {
    "ANTIBIOTIC": "항생제",
    "ANALGESIC_ANTIPYRETIC": "해열진통제",
    "ANTIVIRAL": "항바이러스제",
    "ANTIHISTAMINE": "항히스타민제",
    "RESPIRATORY": "호흡기용제",
    "CORTICOSTEROID": "스테로이드",
}


def lookup(material_code: str, category: Optional[str] = None) -> dict:
    """
    {drug, category, kg_per_1000_units}를 돌려준다.
    표에 없으면 category만으로 최소 정보를 구성한다.
    """
    entry = DRUG_MATERIAL_MAP.get(material_code)
    if entry:
        return dict(entry)

    drug_category = CATEGORY_FALLBACK.get(category or "", "의약품 원료")
    return {
        "drug": f"{drug_category} 원료 ({material_code})",
        "category": drug_category,
        "kg_per_1000_units": None,
    }
