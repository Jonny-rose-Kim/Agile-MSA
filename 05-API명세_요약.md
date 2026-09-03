# 5. API 명세 — 프론트엔드 호출 엔드포인트 목록

- **Base URL**: `http://localhost:8080` (API Gateway) — 프론트는 개별 서비스 포트를 직접 호출하지 않는다
- **인증 헤더**: `Authorization: Bearer {access_token}` (프론트가 첨부) / `X-User-Id`는 **Gateway가 자동 주입**하므로 프론트는 보내지 않는다
- **공통 응답**: `{ "success": true, "message": "성공", "data": { ... } }`

| Sprint | 서비스 | Method | URL | 기능 | 권한 |
|---|---|---|---|---|---|
| 1 | auth-server | GET | `/oauth2/authorize` | 로그인 진입 (브라우저 리다이렉트) | 불필요 |
| 1 | auth-server | POST | `/oauth2/token` | 인가코드 → 액세스 토큰 교환 | Basic 인증 |
| 1 | user-service | POST | `/api/users/register` | 회원가입 (BUYER / SUPPLIER) | 불필요 |
| 1 | user-service | GET | `/api/users/me` | 내 정보 조회 (역할 분기 기준) | 로그인 |
| 1 | user-service | GET | `/api/users/{id}` | 공급사 정보 조회 (GMP 인증 여부 포함) | 로그인 |
| 1 | material-service | GET | `/api/materials` | 원료 카탈로그 목록·검색 | 로그인 |
| 1 | material-service | GET | `/api/materials/{id}` | 원료 상세 조회 | 로그인 |
| 1 | material-service | GET | `/api/materials/code/{materialCode}/suppliers` | **공급 가능 인증 공장 자동 조회 (Ep-01 US1)** | 로그인 |
| 1 | material-service | POST | `/api/materials` | 공급 원료 등록 | SUPPLIER |
| 1 | material-service | PUT | `/api/materials/{id}` | 원료 정보 수정 | SUPPLIER (본인) |
| 1 | material-service | PATCH | `/api/materials/{id}/capacity` | **여유 생산능력 등록·갱신 (Ep-02 US2)** | SUPPLIER (본인) |
| 1 | material-service | GET | `/api/materials/my` | 내 공급 품목 목록 | SUPPLIER |
| 1 | material-service | DELETE | `/api/materials/{id}` | 공급 중단 처리 (Soft Delete) | SUPPLIER (본인) |
| 1 | order-service | POST | `/api/inventories` | 보유 원료 재고·임계치 등록 | BUYER |
| 1 | order-service | GET | `/api/inventories/my` | 내 재고 목록 조회 | BUYER |
| 1 | order-service | PATCH | `/api/inventories/{id}/stock` | 현재 재고량 수정 | BUYER (본인) |
| 1 | order-service | GET | `/api/inventories/alerts` | **재고 부족 감지 알림 조회 (Ep-02 US1)** | BUYER |
| 1 | order-service | POST | `/api/orders` | 조달 신청 (주문 PENDING 생성) | BUYER |
| 1 | order-service | GET | `/api/orders/my` | 내 조달 주문 목록 | BUYER |
| 1 | order-service | GET | `/api/orders/{id}` | 조달 주문 상세 조회 | 로그인 |
| 1 | recommend-service | GET | `/api/recommend` | **AI 수요 예측 + 공급 리스크 기반 공장 추천 (Ep-03 US1)** | 로그인 |
| 2 | payment-service | POST | `/api/payments` | 조달 결제 요청 | BUYER |
| 2 | payment-service | GET | `/api/payments/my` | 내 결제 내역 조회 | BUYER |
| 2 | order-service | GET | `/api/orders/{id}/status` | **주문 상태 실시간 조회 (Ep-01 US2, 폴링용)** | 로그인 |
| 2 | order-service | GET | `/api/orders/supplier` | 공장이 수주한 주문 목록 (상태 필터) | SUPPLIER |

> **HTTP 상태 코드**: `200` 성공 / `201` 생성됨 / `400` 유효성 실패 / `401` 토큰 만료 → 로그인 이동 / `403` 권한 없음 / `404` 없음 / `409` 중복 / `500` 서버 오류
> **내부 전용 API**(`/api/**/internal/**`)는 서비스 간 호출용이며, 프론트엔드는 호출하지 않는다.
> **Kafka 이벤트**(`payment.completed` → 주문 상태 전환·생산능력 차감)는 서버 간 비동기 처리이므로 프론트 호출 대상이 아니며, 프론트는 `/api/orders/{id}/status` 폴링으로 결과를 확인한다.
> **`/api/recommend` 통합**: 수요 예측(`/api/forecast/demand`)과 공장 추천이 기능상 중복되어 단일 엔드포인트로 합쳤다. 한 번 호출로 `predictedDemand`·`confidence`와 `suppliers`(추천 공장)를 함께 받는다.
