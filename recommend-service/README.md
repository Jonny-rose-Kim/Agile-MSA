# recommend-service

원료의약품 수급 매칭 플랫폼 — AI 수요 예측 · 소진 시뮬레이션 · 공급사 추천 (FastAPI, 포트 8085)

```
로그인 제약사의 재고  →  OpenAI 수요 예측(web_search)  →  소진 시뮬레이션  →  부족량 기준 공급사 추천
```

엔드포인트는 `GET /api/recommend` **하나**다. (API Gateway가 `/api/recommend/**`만 이 서비스로 보낸다)

---

## 1. 엔드포인트

```
GET /api/recommend?materialCode=API-ACET-325&quantity=800&days=90&asOf=2025-10-20
Authorization: Bearer <access_token>
```

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `materialCode` | O | 원료코드 |
| `quantity` | X | 필요 수량. **생략하면 권장 발주량을 필요 수량으로 쓴다** |
| `days` | X | 예측 기간(일). 기본 90 |
| `asOf` | X | 기준일 `YYYY-MM-DD`. 데모용. 우선순위 `asOf` > `DEMO_AS_OF` > 오늘 |
| `horizon` | X | `days`의 별칭 (기존 화면 호환) |

### 응답

FastAPI라 `success/message/data` 래퍼가 없다. 프론트의 `unwrap()`이
`res.data.data ?? res.data`이므로 아래 객체가 그대로 화면에 전달된다.

```json
{
  "material": {
    "materialId": 2, "materialCode": "API-ACET-325", "name": "아세트아미노펜 원료",
    "category": "ANALGESIC_ANTIPYRETIC", "unit": "kg",
    "drug": "아세트아미노펜 제제(해열진통제)"
  },
  "inventory": { "inventoryId": 102, "quantity": 2400.0, "threshold": 1000.0 },
  "forecast": {
    "days": 90, "asOf": "2025-10-20", "basis": "LLM_FORECAST",
    "demandChange": 0.25, "confidence": 0.75,
    "summary": "가을철 호흡기 감염병 증가와 예방접종 시작으로 ... 현재 재고 2,400kg는 예측 소진 속도 기준 35일 후(2025-11-24) 임계치 아래로 내려갑니다. 2,240kg 발주를 권장합니다.",
    "dailyConsumption": { "baseline": 32.35, "predicted": 40.44 },
    "shortageDate": "2025-11-24", "stockoutDate": "2025-12-19",
    "projectedQuantity": 0.0, "shortfall": 2239.9,
    "recommendedOrderQty": 2240.0, "recommendedThreshold": 1213.3,
    "factors": [
      { "name": "인플루엔자 의사환자 증가 추세", "trend": "RISING", "impact": "HIGH",
        "evidence": "2025년 39주차 의사환자 분율이 1,000명당 9.0명으로 유행기준(9.1)에 근접",
        "source_url": "https://..." }
    ]
  },
  "suppliers": [
    { "materialId": 2, "supplierId": 7, "supplierName": "한국API공장",
      "gmpCertified": true, "price": 18000.0, "availableCapacity": 9000.0,
      "pastOrderCount": 52, "estimatedCost": 40320000.0 }
  ]
}
```

**`inventory`가 `null`이면** (해당 원료 미보유) `forecast`는 `demandChange` · `confidence` ·
`summary` · `factors`만 채우고 **소진 관련 7개 필드는 전부 `null`**이다.
이때 `suppliers`는 `quantity` 파라미터 기준으로만 추린다.

---

## 2. 수요 예측

### `FORECASTER=llm` — OpenAI Responses API + `web_search`

`app/service/forecast/llm_forecaster.py`

- `temperature=0`, Structured Outputs(`json_schema`, `strict`)
- 입력: 약효군 · 대표 의약품 · 기준일 · 예측 기간 · 계절
- 검증: `demand_change`가 `-0.5 ~ 1.0` 범위인지, `factors`가 1개 이상인지
- **실패·타임아웃 시 예외를 던지지 않고 `SYNTHETIC_TREND`로 폴백**한다
- 캐시: `(약효군, 기준일, 기간, 모델)` 키로 6시간 파일 캐시 (`.forecast-cache/`)
- `basis` = `LLM_FORECAST`

> **타임아웃 주의**: `web_search`를 쓰면 실측 **9.3초**가 나왔다. 기본 `OPENAI_TIMEOUT=10`은
> 여유가 거의 없어 네트워크가 조금만 느려도 폴백된다. 데모라면 `20~30`을 권장한다.
> 캐시가 적중하면 API를 타지 않으므로 두 번째 호출부터는 즉시 응답한다.

### `FORECASTER=mock` — 합성 데이터 추세

기존 합성 데이터 코드(`app/service/forecast_service.py`, `app/data/synthetic_generator.py`)를
지우지 않고 폴백 경로로 남겨뒀다. OpenAI를 전혀 호출하지 않는다. `basis` = `SYNTHETIC_TREND`.

---

## 3. 약-원료 매핑 (`app/data/drug_material_map.py`)

LLM에게 "이 원료가 무슨 약이 되는지" 알려주는 고정 테이블. mock 원료 6종을 전부 채웠다.

| 원료코드 | 대표 완제의약품 | 약효군 | kg/1,000단위 |
|---|---|---|---|
| API-CEFA-500 | 세파클러 캡슐 500mg | 항생제 | 0.500 |
| API-ACET-325 | 아세트아미노펜 제제 | 해열진통제 | 0.325 |
| API-OSEL-075 | 오셀타미비르 캡슐 75mg | 항바이러스제 | 0.075 |
| API-LORA-010 | 로라타딘 정 10mg | 항히스타민제 | 0.010 |
| API-AMBR-030 | 암브록솔 정 30mg | 호흡기용제 | 0.030 |
| API-DEXA-004 | 덱사메타손 정 0.5mg | 스테로이드 | 0.0005 |

표에 없는 원료는 material-service의 `category`만으로 폴백한다.

---

## 4. 소진 시뮬레이션 (`app/service/consumption_service.py`)

```
평상시 일 소진량 = order-service 실이력 최근 90일 발주량 / 90
                  (없으면 합성 demand_history() 최근 90일 평균)
예측 일 소진량   = 평상시 × (1 + demandChange)

days일간 하루 단위로 재고를 깎으며
  shortageDate      : 재고가 threshold 아래로 내려가는 날
  stockoutDate      : 재고가 0이 되는 날
  projectedQuantity : days 끝 시점 예상 재고

shortfall            = max(0, threshold + 예측 일 소진량 × days − 현재 재고)
recommendedOrderQty  = ceil(shortfall)            (발주는 쪼갤 수 없으므로 올림)
recommendedThreshold = 예측 일 소진량 × LEAD_TIME_DAYS
```

---

## 5. 공급사 추천

- **필요 수량** = `quantity` 파라미터, 없으면 `recommendedOrderQty`
- **필터**: GMP 인증 보유 **AND** `availableCapacity >= 필요 수량`
- **정렬**: 과거 거래 횟수 내림차순 → 단가 오름차순
- `estimatedCost = price × 필요 수량`

> 리스크 점수 · 등급 · 추천 사유 필드는 **응답에서 제거**했다.

---

## 6. `summary` 구성

OpenAI가 준 `summary`를 그대로 쓰고, 소진 결과 한 문장을 템플릿으로 덧붙인다.

```
[LLM] 가을철 호흡기 감염병 증가와 예방접종 시작으로 아세트아미노펜 제제 수요는
      평상시보다 증가할 것으로 보입니다. 향후 90일간 수요는 약 25% 증가할 것으로 예측됩니다.
[템플릿] 현재 재고 2,400kg는 예측 소진 속도 기준 35일 후(2025-11-24) 임계치 아래로
        내려갑니다. 2,240kg 발주를 권장합니다.
```

---

## 7. mock 모드

`material-service` / `order-service`가 아직 없어서 **기본이 `MOCK_MODE=true`**다.
mock 데이터는 `app/data/mock_catalog.py` 한 곳에 모여 있다.

> **여기 값은 전부 데모용 고정값이다.** 실제 공장·재고·거래 이력 조회 결과가 아니다.

| 항목 | 내용 |
|---|---|
| 원료 | 6종. 재고·임계치 전부 등록 |
| 재고 | `threshold` ≈ 30일치, `quantity` ≈ 70~75일치 → 90일 구간 안에서 하회일이 잡힌다 |
| 공급자 | 원료당 2~3곳. 국내 인증 / 인도 인증 / 중국 미인증 혼합 |
| 과거 거래 | 195건. 공급자-원료 쌍 15개, 횟수 제각각, 전부 `2025-10-10` 이전 |

두 서비스가 뜨면 compose에 아래만 넣으면 **코드 수정 없이** 실서비스로 붙는다.

```yaml
- MOCK_MODE=false
- MATERIAL_SERVICE_URL=http://material-service:8086
- ORDER_SERVICE_URL=http://order-service:8087
```

---

## 8. 환경변수

`.env.example`을 복사해서 쓴다. **`.env`는 `.gitignore`에 있어 커밋되지 않는다.**

```bash
cp .env.example .env    # OPENAI_API_KEY 채우기
```

| 변수 | 기본값 | 설명 |
|---|---|---|
| `FORECASTER` | `llm` | `llm` = OpenAI + web_search, `mock` = 합성 추세 |
| `OPENAI_API_KEY` | (없음) | 미설정이면 자동으로 `SYNTHETIC_TREND` 폴백 |
| `OPENAI_MODEL` | `gpt-4o` | |
| `OPENAI_TIMEOUT` | `10` | 초. web_search는 9초 이상 걸리니 20~30 권장 |
| `FORECAST_CACHE_TTL_HOURS` | `6` | LLM 예측 파일 캐시 수명 |
| `LEAD_TIME_DAYS` | `30` | 권장 임계치 = 예측 일 소진량 × 이 값 |
| `FORECAST_DEFAULT_DAYS` | `90` | `days` 미지정 시 기본 |
| `MOCK_MODE` | `true` | mock 데이터 사용 |
| `MATERIAL_SERVICE_URL` | `http://material-service:8086` | |
| `ORDER_SERVICE_URL` | `http://order-service:8087` | |
| `DEMO_AS_OF` | (없음) | 데모용 고정 기준일. `asOf` 파라미터가 우선 |
| `MOCK_CONFIDENCE` | (없음) | 켜면 `confidence`를 이 고정값으로 덮어쓴다 |
| `RECOMMEND_MAX_COUNT` | `5` | 추천 공급사 최대 개수 |

> ⚠️ Dockerfile이 `COPY . .`로 `.env`를 이미지에 굽는다. 로컬 데모 전용이며,
> 배포에서는 compose `environment`나 시크릿 매니저로 옮겨야 한다. (팀 요청 목록)

---

## 9. 실행 · 테스트

```bash
# 전체 스택과 함께 (msa-MyService/ 에서)
docker compose up -d --build recommend-service

# 자체 점검 (OpenAI 호출 없음)
docker compose exec recommend-service python test_skeleton.py

# Swagger
open http://localhost:8085/docs
```

```bash
# 데모: ILI 급상승 시점
curl "http://localhost:8080/api/recommend?materialCode=API-ACET-325&days=90&asOf=2025-10-20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 10. 알려진 제약

- **`demandChange`는 LLM의 추정치다.** 웹 검색 근거를 붙이지만 검증된 예측 모델이 아니다.
  `basis`가 `LLM_FORECAST`인지 `SYNTHETIC_TREND`인지로 출처를 구분한다.
- **평상시 일 소진량이 합성 데이터다** (`MOCK_MODE=true`인 동안).
  order-service 실주문 이력이 붙으면 `consumption_service.baseline_daily_consumption()`이
  자동으로 실이력을 쓴다.
- **합성 데이터 구간은 `2025-07-14 ~ 2026-08-16`**이다. 그 밖의 `asOf`는 양끝으로 당긴다.
- **캐시 키에 원료코드가 없다.** 약효군이 같으면 예측을 공유한다
  (해열진통제 원료가 여러 개면 같은 `demandChange`를 받는다).
