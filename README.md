# 원료의약품 수급 매칭 플랫폼

제약사·연구실이 보유 원료의 **재고 부족을 미리 감지**하고, 그 원료를 실제로 공급할 수 있는
**GMP 인증 공장을 즉시 찾아 조달**하는 B2B 플랫폼입니다.

Spring Boot 3 기반 MSA + Vue 3 SPA로 구현했습니다.
Eureka 서비스 디스커버리, Spring Cloud Gateway, Spring Authorization Server(OAuth2),
Kafka 이벤트, 서비스별 독립 데이터베이스를 사용합니다.

---

## 1. 실행 전 반드시 확인 — 저장소만 클론하면 실행되지 않습니다

`auth-server`와 `api-gateway` **두 개는 강의에서 제공한 프리빌트 이미지**이고 Docker Hub에 없습니다.
이미지 tar 파일은 용량이 커서(343MB / 1.2GB) GitHub에 올릴 수 없어 저장소에서 제외했습니다.

**강의 자료의 이미지 tar 파일이 필요합니다.**

| 파일 | 용량 | 대상 | 포함 이미지 |
|---|---|---|---|
| `infra-images.tar` | 343MB | Intel Mac / Windows (amd64) | auth-server, api-gateway |
| `msa-lecture-images-arm64.tar` | 1.2GB | Apple Silicon (arm64) | 위 2개 + 나머지 전부 |

나머지 9개 서비스는 이 저장소의 소스에서 직접 빌드됩니다.

---

## 2. 사전 준비물

- Docker Desktop (Compose v2 포함) — **메모리 8GB 이상 할당 권장**
- Node.js 20 이상
- 포트 여유: `3000` `8080` `8081` `8083` `8084` `8085` `8086` `8087` `8761` `9000` `3379` `9092`

---

## 3. 실행 순서

```bash
# 1) 클론
git clone https://github.com/Jonny-rose-Kim/Agile-MSA.git
cd Agile-MSA/msa-MyService

# 2) 강의 제공 이미지 tar를 이 폴더(msa-MyService/)에 복사한 뒤 로드
docker load -i msa-lecture-images-arm64.tar   # Apple Silicon
# docker load -i infra-images.tar             # Intel Mac / Windows

# 3) 백엔드 빌드 및 기동
#    첫 실행은 Gradle 의존성 내려받느라 10분 이상 걸릴 수 있습니다.
docker compose up -d --build

# 4) 기동 확인 — 8개 서비스가 모두 등록되면 준비 완료 (1~2분 소요)
open http://localhost:8761

# 5) 프론트엔드 (새 터미널)
cd ../vue-frontend
npm install
npm run dev
```

> **기동 직후 1분간은 로그인이 503으로 실패할 수 있습니다.**
> API Gateway가 Eureka 레지스트리를 갱신하는 주기 때문이며, 잠시 기다리면 정상화됩니다.

---

## 4. 접속 주소

| 대상 | 주소 | 설명 |
|---|---|---|
| **프론트엔드** | **http://localhost:3000** | **여기서 시작하세요** |
| 인증 서버 | http://localhost:8080/login | OAuth2 로그인 페이지 (직접 접속할 일은 없음) |
| Eureka 대시보드 | http://localhost:8761 | 서비스 등록 현황 확인 |
| Swagger UI | http://localhost:8086/swagger-ui.html | 서비스별 API 문서 (포트만 바꿔서 접근) |

---

## 5. 테스트 계정

`docker compose up` 시 자동으로 생성됩니다. 비밀번호는 둘 다 `password1234`입니다.

| 이메일 | 비밀번호 | 역할 | 로그인 후 첫 화면 |
|---|---|---|---|
| `student@lecture.com` | `password1234` | **제약사 · 연구실** (BUYER) | 원료 카탈로그 |
| `instructor@lecture.com` | `password1234` | **공급 공장** (SUPPLIER) | 내 공급 품목 |

회원가입도 동작합니다. 계정 유형을 고르면 그에 맞는 메뉴가 나옵니다.

> 로그인은 인증 서버로 이동했다가 돌아오는 **OAuth2 Authorization Code Flow**입니다.
> "로그인" 버튼을 누르면 `localhost:8080`으로 이동하는 것이 정상입니다.

---

## 6. 채점 시나리오 — 5분 안에 전 기능 확인하기

**제약사 계정(`student@lecture.com`)으로 로그인**한 뒤 순서대로 따라가면 됩니다.
데모용 원료 10건이 미리 들어 있습니다.

### ① 원료 카탈로그 · 공급처 비교 — Ep-01 US1

`원료 카탈로그` → `API-CEFA-500`(세파졸린나트륨) **상세** 클릭 → **공급처 찾기**

같은 원료를 **두 공장이 다른 조건으로 공급**하는 것이 보입니다.

| 공장 | 단가 | 리드타임 | 인증 |
|---|---|---|---|
| 한국API공장 | 42,000원 | 14일 | GMP, DMF |
| 대한파마텍 | 39,500원 | 21일 | GMP |

"싸지만 오래 걸리는 곳"과 "비싸지만 빠른 곳"을 비교해 고르는 것이 이 서비스의 핵심입니다.

### ② 조달 신청 — Ep-01 US2

`조달 주문` → 값이 채워져 있음 → **조달 신청**

주문이 `PENDING`으로 생성됩니다. `원료 카탈로그`로 돌아가면
**여유 생산능력이 주문한 수량만큼 줄어 있습니다.** (order-service → material-service 내부 연동)

### ③ 재고 등록 · 부족 감지 — Ep-02 US1

`재고 관리` → 원료코드 `EXC-LAC-200`, 현재 재고량 `100`, 부족 임계치 `5000` → **등록**

임계치보다 재고가 적으므로 즉시 `부족`으로 표시됩니다.
`부족 알림`으로 가면 심각도·부족량과 함께 잡혀 있습니다.
(백그라운드 스케줄러가 60초마다 재검사합니다)

### ④ 결제 · 주문 확정 — Sprint 2

`결제 내역` → 결제 대기 주문번호가 미리 채워져 있음 → **결제하기**

결제 후 `조달 주문`으로 이동하면 **몇 초 뒤 주문이 `CONFIRMED`로 바뀝니다.**
동기 호출이 아니라 **Kafka 이벤트**(`order.payment.completed`)로 처리하기 때문에 시차가 있습니다.

### ⑤ AI 수요 예측 · 공장 추천 — Ep-03

`AI 추천` → 조건이 채워져 있음 → **조회**

수요 증감률, 재고 소진 시뮬레이션(임계치 하회일·소진일), 권장 발주량,
그리고 그 물량을 댈 수 있는 GMP 인증 공장이 함께 나옵니다.

> 이 화면은 **데모 데이터로 동작합니다**(화면 상단에 명시). 수요 예측의 근거인 발주 이력이
> 아직 합성 데이터라 `MOCK_MODE=true`가 기본입니다.
> `FORECASTER=llm` + `OPENAI_API_KEY`를 주면 OpenAI 웹 검색 기반 예측으로 전환됩니다.

### ⑥ 공급 공장 입장에서 보기

**로그아웃 후 `instructor@lecture.com`으로 로그인**

- `내 공급 품목` — 여유 생산능력을 표에서 바로 수정 (Ep-02 US2)
- `수주 관리` — ②에서 넣은 주문이 구매처 이름과 함께 보입니다

권한이 분리돼 있어 **다른 공장의 주문은 조회되지 않습니다.**

---

## 7. 서비스 구성

| 서비스 | 포트 | 담당 도메인 | 데이터베이스 |
|---|---|---|---|
| api-gateway | 8080 | 라우팅 · JWT 검증 · `X-User-Id` 주입 | — |
| auth-server | 9000 | OAuth2 인가 · 토큰 발급 | `lecture_db` |
| eureka-server | 8761 | 서비스 레지스트리 | — |
| user-service | 8081 | 회원 · 권한 (BUYER / SUPPLIER) | `lecture_db` |
| material-service | 8086 | 원료 카탈로그 · 공급 가능 공장 · 여유 생산능력 | `material_db` |
| order-service | 8087 | 조달 주문 · 재고 · 부족 감지 · 수주 관리 | `order_db` |
| payment-service | 8084 | 조달 결제 · 결제 완료 이벤트 발행 | `payment_db` |
| recommend-service | 8085 | AI 수요 예측 · 소진 시뮬레이션 · 공장 추천 | — (FastAPI) |
| vue-frontend | 3000 | 프론트엔드 | — |

프론트엔드는 **Gateway(8080)만** 호출합니다. 개별 서비스 포트를 직접 부르지 않습니다.

`enrollment-service`(8083)는 강의 템플릿에서 남은 서비스로, 이 프로젝트 도메인과 무관합니다.

### 서비스별 데이터베이스

각 서비스가 자기 스키마만 소유합니다(`init-db/00_databases.sql`이 자동 생성).
`lecture_db`를 공유하는 것은 `user-service`와 `auth-server`뿐인데,
auth-server가 프리빌트 이미지라 같은 `users` 테이블을 봐야 해서 분리할 수 없습니다.

### 결제 → 주문 확정 (비동기)

```
POST /api/payments      payment-service : 주문 금액 확인 → 승인 → 이벤트 발행
        ↓  order.payment.completed (Kafka)
order-service           : 주문 PENDING → CONFIRMED
        ↓
주문 상세 화면           : 폴링으로 상태 변화 감지
```

동기 호출로 묶지 않은 이유는, 결제는 성공했는데 주문 확정이 실패했다고 해서
결제를 되돌릴 수 없기 때문입니다. 이벤트로 끊어 각자 재시도할 수 있게 했습니다.

---

## 8. 담당 분배

| 담당 | 영역 |
|---|---|
| 김재환 | 프론트엔드 전체 · 로그인/회원가입(`/api/users`) · 조달 주문 · 결제 · 서비스 통합 |
| 이서홍 | `material-service` (원료 카탈로그 · 공급 가능 공장) |
| ljs65626 | `order-service` 재고 영역 (보유 재고 · 임계치 · 부족 감지 스케줄러) |
| place0 | `recommend-service` (AI 수요 예측 · 소진 시뮬레이션 · 공급사 추천) |

협업은 브랜치 → Pull Request → 리뷰 → 머지로 진행했습니다.
같은 서비스를 두 사람이 각자 구현해 충돌한 건은 PR #4에서 서비스 단위로 정리했습니다.

---

## 9. 문서

| 문서 | 내용 |
|---|---|
| [**조별 발표자료 (PDF)**](Agile+MSA_조별발표_3반_2조.pdf) | **3반 2조 최종 발표자료** |
| [05-API명세.md](05-API명세.md) | 전체 API 상세 명세 (Sprint 1 전체 + Sprint 2 E2E) |
| [05-API명세_요약.md](05-API명세_요약.md) | 엔드포인트 한 장 요약표 |
| [06-프론트엔드-API배선흐름.md](06-프론트엔드-API배선흐름.md) | 화면 ↔ API 배선 흐름도 |
| [vue-frontend/FRONTEND_GUIDE.md](msa-MyService/vue-frontend/FRONTEND_GUIDE.md) | 프론트엔드 구조 · 디자인 시스템 규약 |
| [material-service/README.md](msa-MyService/material-service/README.md) | 원료 서비스 설계 |
| [recommend-service/README.md](msa-MyService/recommend-service/README.md) | 수요 예측 알고리즘 · 환경변수 |

---

## 10. 트러블슈팅

### 로그인 버튼을 눌렀는데 "페이지를 로드하지 못함"

`auth-server`가 안 떠 있는 경우입니다. 로그인은 브라우저를 `localhost:8080`으로
이동시키므로, 컨테이너가 없으면 브라우저 단계에서 실패합니다.

```bash
docker compose ps          # auth-server 가 Up 인지 확인
docker compose logs auth-server
```

프리빌트 이미지를 로드하지 않았다면 §1을 다시 확인하세요.

### 기동 직후 API가 503

Eureka 레지스트리 갱신 대기입니다. 1분 정도 기다리면 해결됩니다.
`http://localhost:8761`에서 서비스 8개가 모두 보이면 준비 완료입니다.

### 빌드 중 `429 Too Many Requests`

Maven Central이 같은 공인 IP의 동시 빌드를 제한한 것입니다.
Google 미러를 우선 조회하도록 설정해 뒀지만, 그래도 나면 잠시 후 다시 시도하세요.
`--no-cache`는 붙이지 마세요. 매번 전부 다시 받아 429에 더 잘 걸립니다.

### 포트 충돌 — `address already in use`

```bash
lsof -i :8080          # 점유 중인 프로세스 확인
```

해당 앱을 종료하거나, `docker-compose.override.yml`로 호스트 포트만 바꿔 주세요.

### 완전히 초기화하고 다시 시작

```bash
docker compose down -v     # 볼륨까지 삭제 (데이터 초기화)
docker compose up -d --build
```

데이터베이스와 시드 데이터가 자동으로 다시 만들어집니다.
