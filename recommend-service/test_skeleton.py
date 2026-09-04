"""
자체 점검. 네트워크·Kafka·Eureka·OpenAI를 타지 않는다 (MOCK_MODE + FORECASTER=mock 기준).

실행:
    docker compose exec recommend-service python test_skeleton.py
"""

from datetime import date

import numpy as np
import pandas as pd
from fastapi.testclient import TestClient

from app.config import settings as settings_module
from app.config.security import verify_token
from app.data import drug_material_map, mock_catalog, synthetic_generator as synth
from app.service import consumption_service
from app.service.forecast import llm_forecaster
from main import app

# 인증은 auth-server를 타므로 테스트에서만 우회한다.
app.dependency_overrides[verify_token] = lambda: {"sub": "1", "email": "test@lecture.com"}
client = TestClient(app)   # with 블록을 쓰지 않으므로 lifespan(Eureka/Kafka)은 실행되지 않는다

AS_OF = date(2025, 10, 20)


class forecaster_mode:
    """FORECASTER 설정을 임시로 바꾼다."""

    def __init__(self, mode, **overrides):
        self.mode, self.overrides = mode, overrides

    def __enter__(self):
        self.saved = {"forecaster": settings_module.settings.forecaster}
        settings_module.settings.forecaster = self.mode
        for k, v in self.overrides.items():
            self.saved[k] = getattr(settings_module.settings, k)
            setattr(settings_module.settings, k, v)
        return self

    def __exit__(self, *exc):
        for k, v in self.saved.items():
            setattr(settings_module.settings, k, v)


def get(path):
    res = client.get(path)
    assert res.status_code == 200, res.text
    return res.json()


# ---------------------------------------------------------------- 합성 데이터

def test_external_factors_shape():
    """날짜 수가 구간과 일치하고, 음수가 없어야 한다."""
    df = synth.external_factors()
    assert len(df) == (synth.END_DATE - synth.START_DATE).days + 1
    assert (df["ili"] >= 0).all()
    assert (df["resp_index"] >= 0).all()
    assert df["ili"].max() > synth.ILI_EPIDEMIC_THRESHOLD
    assert df.attrs["ili_epidemic_threshold"] == 9.1


def test_analgesic_demand_correlates_with_lagged_ili():
    """해열진통제 계열 7일 롤링 발주량 vs ILI 14일 지연값의 피어슨 상관 >= 0.6."""
    materials = mock_catalog.MOCK_MATERIALS
    history = synth.demand_history(materials)
    target = next(m for m in materials if m["category"] == "ANALGESIC_ANTIPYRETIC")
    series = history[history["materialCode"] == target["materialCode"]].set_index("date")

    ili_lagged = synth.external_factors().set_index("date")["ili"].shift(14)
    joined = pd.concat([series["rolling7"], ili_lagged.rename("ili")], axis=1).dropna()
    corr = float(np.corrcoef(joined["rolling7"], joined["ili"])[0, 1])
    assert corr >= 0.6, f"상관계수 {corr:.3f}"


# ---------------------------------------------------------------- 약-원료 매핑

def test_drug_material_map_covers_all_mock_materials():
    for m in mock_catalog.MOCK_MATERIALS:
        entry = drug_material_map.lookup(m["materialCode"], m["category"])
        assert entry["drug"], m["materialCode"]
        assert entry["category"], m["materialCode"]

    # 표에 없는 원료는 category만으로 폴백
    fallback = drug_material_map.lookup("API-UNKNOWN-999", "ANTIBIOTIC")
    assert fallback["category"] == "항생제"
    assert fallback["kg_per_1000_units"] is None


# ---------------------------------------------------------------- 소진 시뮬레이션

def test_higher_demand_brings_shortage_date_forward():
    """demand_change +0.3이면 0일 때보다 임계치 하회일이 앞당겨진다."""
    common = dict(quantity=2400, threshold=1000, baseline_daily=32.0, days=90, as_of=AS_OF)

    flat = consumption_service.simulate(demand_change=0.0, **common)
    surge = consumption_service.simulate(demand_change=0.3, **common)

    assert surge["shortageDate"] < flat["shortageDate"], (
        f"{surge['shortageDate']} 가 {flat['shortageDate']} 보다 빨라야 한다"
    )
    assert surge["stockoutDate"] < flat["stockoutDate"]
    assert surge["dailyConsumption"]["predicted"] > flat["dailyConsumption"]["predicted"]
    assert surge["projectedQuantity"] <= flat["projectedQuantity"]


def test_shortfall_and_order_qty_formula():
    """부족량 = max(0, 임계치 + 예측소진 × days − 현재고), 발주량은 올림."""
    sim = consumption_service.simulate(
        quantity=2400, threshold=1000, baseline_daily=32.0,
        demand_change=0.0, days=90, as_of=AS_OF,
    )
    expected = 1000 + 32.0 * 90 - 2400          # = 1480
    assert sim["shortfall"] == round(expected, 1)
    assert sim["recommendedOrderQty"] == 1480

    # 올림 확인
    odd = consumption_service.simulate(
        quantity=0, threshold=0, baseline_daily=1.5,
        demand_change=0.0, days=3, as_of=AS_OF,
    )
    assert odd["shortfall"] == 4.5 and odd["recommendedOrderQty"] == 5

    # 재고가 충분하면 부족량 0
    plenty = consumption_service.simulate(
        quantity=99999, threshold=100, baseline_daily=1.0,
        demand_change=0.0, days=30, as_of=AS_OF,
    )
    assert plenty["shortfall"] == 0 and plenty["recommendedOrderQty"] == 0
    assert plenty["shortageDate"] is None and plenty["stockoutDate"] is None


def test_recommended_threshold_is_lead_time_of_predicted():
    sim = consumption_service.simulate(
        quantity=1000, threshold=100, baseline_daily=10.0,
        demand_change=0.5, days=30, as_of=AS_OF,
    )
    # 예측 일 소진 15.0 × 리드타임 30일
    assert sim["recommendedThreshold"] == 15.0 * settings_module.settings.lead_time_days


# ---------------------------------------------------------------- 예측기 전환

def test_forecaster_mock_uses_synthetic_and_calls_no_openai():
    """FORECASTER=mock 이면 basis가 SYNTHETIC_TREND이고 OpenAI를 부르지 않는다."""
    called = []
    original = llm_forecaster.forecast
    llm_forecaster.forecast = lambda *a, **k: called.append(1)
    try:
        with forecaster_mode("mock"):
            body = get(f"/api/recommend?materialCode=API-ACET-325&days=90&asOf={AS_OF}")
    finally:
        llm_forecaster.forecast = original

    assert body["forecast"]["basis"] == "SYNTHETIC_TREND"
    assert not called, "mock 모드에서 OpenAI를 호출하면 안 된다"


def test_forecaster_llm_uses_mocked_response():
    """LLM 응답을 가짜로 주입해 basis·factors가 그대로 실리는지 본다."""
    fake = {
        "demandChange": 0.3,
        "confidence": 0.8,
        "summary": "인플루엔자 유행으로 해열진통제 수요가 증가할 전망입니다.",
        "factors": [{"name": "인플루엔자 유행", "trend": "RISING", "impact": "HIGH",
                     "evidence": "표본감시 의사환자분율 상승", "source_url": "https://kdca.go.kr/"}],
        "basis": "LLM_FORECAST",
    }
    original = llm_forecaster.forecast
    llm_forecaster.forecast = lambda *a, **k: fake
    try:
        with forecaster_mode("llm"):
            body = get(f"/api/recommend?materialCode=API-ACET-325&days=90&asOf={AS_OF}")
    finally:
        llm_forecaster.forecast = original

    f = body["forecast"]
    assert f["basis"] == "LLM_FORECAST"
    assert f["demandChange"] == 0.3
    assert f["confidence"] == 0.8
    assert f["factors"][0]["source_url"] == "https://kdca.go.kr/"
    assert "인플루엔자" in f["summary"]
    # summary 뒤에 소진 문장이 붙는다
    assert "발주를 권장합니다" in f["summary"] or "임계치를 밑돌지 않습니다" in f["summary"]


def test_llm_failure_falls_back_to_synthetic():
    """LLM이 None을 주면 SYNTHETIC_TREND로 폴백한다."""
    original = llm_forecaster.forecast
    llm_forecaster.forecast = lambda *a, **k: None
    try:
        with forecaster_mode("llm"):
            body = get(f"/api/recommend?materialCode=API-ACET-325&days=90&asOf={AS_OF}")
    finally:
        llm_forecaster.forecast = original

    assert body["forecast"]["basis"] == "SYNTHETIC_TREND"


def test_llm_validation_rejects_bad_payloads():
    """범위 이탈·factors 누락은 폴백 대상이다."""
    assert llm_forecaster._validate({"demand_change": 5.0, "factors": [{}]}) is None
    assert llm_forecaster._validate({"demand_change": -0.9, "factors": [{}]}) is None
    assert llm_forecaster._validate({"demand_change": 0.2, "factors": []}) is None
    assert llm_forecaster._validate({"factors": [{}]}) is None

    ok = llm_forecaster._validate({
        "demand_change": 0.25, "confidence": 0.7, "summary": "s",
        "factors": [{"name": "n", "trend": "RISING", "impact": "HIGH",
                     "evidence": "e", "source_url": "u"}],
    })
    assert ok["demandChange"] == 0.25 and ok["basis"] == "LLM_FORECAST"
    # confidence는 0~1로 자른다
    assert llm_forecaster._validate({
        "demand_change": 0.1, "confidence": 9, "factors": [{"trend": "X", "impact": "Y"}],
    })["confidence"] == 1.0


# ---------------------------------------------------------------- 응답 계약

def test_response_schema():
    with forecaster_mode("mock"):
        body = get(f"/api/recommend?materialCode=API-ACET-325&quantity=800&days=90&asOf={AS_OF}")

    assert set(body) == {"material", "inventory", "forecast", "suppliers"}
    assert set(body["material"]) == {"materialId", "materialCode", "name", "category", "unit", "drug"}
    assert set(body["inventory"]) == {"inventoryId", "quantity", "threshold"}
    assert set(body["forecast"]) == {
        "days", "asOf", "basis", "demandChange", "confidence", "summary",
        "dailyConsumption", "shortageDate", "stockoutDate", "projectedQuantity",
        "shortfall", "recommendedOrderQty", "recommendedThreshold", "factors",
    }
    assert set(body["forecast"]["dailyConsumption"]) == {"baseline", "predicted"}
    for f in body["forecast"]["factors"]:
        assert set(f) == {"name", "trend", "impact", "evidence", "source_url"}

    assert body["suppliers"]
    for s in body["suppliers"]:
        assert set(s) == {
            "materialId", "supplierId", "supplierName", "gmpCertified",
            "price", "availableCapacity", "pastOrderCount", "estimatedCost",
        }
        assert s["gmpCertified"] is True, "GMP 미인증은 걸러져야 한다"
        assert s["availableCapacity"] >= 800
        assert s["estimatedCost"] == s["price"] * 800

    assert body["forecast"]["asOf"] == str(AS_OF)


def test_inventory_null_leaves_consumption_fields_null():
    """재고가 없으면 소진 관련 필드는 null이고 suppliers는 quantity 파라미터 기준."""
    with forecaster_mode("mock"):
        body = get(f"/api/recommend?materialCode=API-UNKNOWN-999&quantity=500&days=30&asOf={AS_OF}")

    assert body["inventory"] is None
    f = body["forecast"]
    for key in ("dailyConsumption", "shortageDate", "stockoutDate",
                "projectedQuantity", "shortfall", "recommendedOrderQty", "recommendedThreshold"):
        assert f[key] is None, key
    # 예측 자체는 살아 있다
    assert f["demandChange"] is not None
    assert f["factors"]
    assert body["suppliers"] == []      # 카탈로그에 없는 원료라 후보 없음


def test_quantity_defaults_to_recommended_order_qty():
    """quantity를 안 주면 권장 발주량이 필요 수량이 된다."""
    with forecaster_mode("mock"):
        body = get(f"/api/recommend?materialCode=API-ACET-325&days=90&asOf={AS_OF}")

    need = body["forecast"]["recommendedOrderQty"]
    assert need > 0
    for s in body["suppliers"]:
        assert s["availableCapacity"] >= need
        assert s["estimatedCost"] == s["price"] * need


def test_as_of_shifts_reference_date():
    with forecaster_mode("mock"):
        a = get("/api/recommend?materialCode=API-ACET-325&days=30&asOf=2025-10-20")["forecast"]
        b = get("/api/recommend?materialCode=API-ACET-325&days=30&asOf=2026-08-10")["forecast"]
    assert a["asOf"] == "2025-10-20" and b["asOf"] == "2026-08-10"
    assert a["dailyConsumption"]["baseline"] != b["dailyConsumption"]["baseline"]


def test_horizon_alias():
    with forecaster_mode("mock"):
        assert get(f"/api/recommend?materialCode=API-ACET-325&horizon=30&asOf={AS_OF}")["forecast"]["days"] == 30


def test_all_mock_materials_resolve():
    with forecaster_mode("mock"):
        for m in mock_catalog.MOCK_MATERIALS:
            body = get(f"/api/recommend?materialCode={m['materialCode']}&days=90&asOf={AS_OF}")
            assert body["inventory"] is not None, m["materialCode"]
            assert body["material"]["drug"], m["materialCode"]
            assert body["forecast"]["shortageDate"], m["materialCode"]


if __name__ == "__main__":
    for name, fn in sorted(globals().items()):
        if name.startswith("test_") and callable(fn):
            fn()
            print(f"  ok  {name}")
    print("\n전부 통과")
