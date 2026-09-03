# 5. API 명세 (프론트엔드 호출 기준)

> 원료의약품 수급 매칭 플랫폼 / 프론트엔드가 실제로 호출하는 엔드포인트 목록
> 작성 기준: **Sprint 1은 전체 상세(Method·URL·Request·Response), Sprint 2는 기능별 End-to-End 흐름만**

---

## 5-0. 공통 규약

### (1) Base URL — 모든 요청은 API Gateway 한 곳으로만

| 구분 | 값 | 비고 |
|---|---|---|
| 운영/로컬 Base URL | `http://localhost:8080` | API Gateway |
| 프론트 개발 서버 | `http://localhost:3000` | Vite Dev Server |
| 실제 axios 호출 | `/api/...` (상대 경로) | Vite proxy가 8080으로 전달 |

> **프론트는 개별 서비스 포트(8081~8085)를 절대 직접 호출하지 않는다.**
> 서비스 포트는 Swagger로 명세를 확인할 때만 사용한다.

```js
// vite.config.js — 프록시 설정 (CORS 회피)
server: {
  port: 3000,
  proxy: {
    '/api':     { target: 'http://localhost:8080', changeOrigin: true },
    '/oauth2':  { target: 'http://localhost:8080', changeOrigin: true },
    '/logout':  { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

### (2) 서비스 · 포트 · Swagger UI 맵

| 서비스 | 포트 | 담당 도메인 | Swagger UI |
|---|---|---|---|
| api-gateway | 8080 | 라우팅 / JWT 검증 / `X-User-Id` 주입 | - |
| auth-server | 9000 | OAuth2 인가·토큰 발급 | - |
| user-service | 8081 | 회원, 권한(BUYER/SUPPLIER) | `http://localhost:8081/swagger-ui/index.html` |
| material-service | 8082 | 원료 카탈로그, 공급 가능 공장 | `http://localhost:8082/swagger-ui/index.html` |
| order-service | 8083 | 재고·임계치, 조달 주문 | `http://localhost:8083/swagger-ui/index.html` |
| payment-service | 8084 | 조달 결제 | `http://localhost:8084/swagger-ui/index.html` |
| recommend-service | 8085 | AI 수요예측·공장 추천 (FastAPI) | `http://localhost:8085/docs` |

### (3) 인증 헤더 규약

| 헤더 | 누가 넣는가 | 값 |
|---|---|---|
| `Authorization` | **프론트엔드** | `Bearer {access_token}` |
| `X-User-Id` | **API Gateway** (JWT에서 추출해 주입) | 숫자 userId |
| `Content-Type` | 프론트엔드 | `application/json` |

> ⚠️ **`X-User-Id`는 프론트가 절대 직접 보내지 않는다.** Gateway가 JWT를 검증한 뒤 백엔드로 주입하는 값이다.
> 프론트가 임의로 보내면 위조가 되며, Gateway가 덮어쓰거나 401이 발생한다.
> 따라서 "내 정보 / 내 재고 / 내 공급품목" 조회 API는 **경로에 userId를 넣지 않고 `/me`, `/my` 형태**로 설계한다.

### (4) 공통 응답 포맷 (Spring 계열 서비스 전체 동일)

```json
{
  "success": true,
  "message": "성공",
  "data": { }
}
```

```js
// 프론트 언래핑 헬퍼 — 모든 화면에서 res.data.data를 직접 파지 않도록 통일
const unwrap = (res) => res?.data?.data ?? res?.data
```

### (5) 공통 에러 응답 / HTTP 상태 코드

```json
{
  "success": false,
  "message": "존재하지 않는 원료입니다: 999",
  "data": null
}
```

| 코드 | 의미 | 프론트 처리 |
|---|---|---|
| 200 / 201 | 성공 / 생성됨 | 정상 처리 |
| 400 | 유효성 실패 (필수값·형식) | 폼 필드 하단에 `message` 표시 |
| 401 | 토큰 없음·만료 | 토큰 삭제 → `/login` 리다이렉트 |
| 403 | 권한 없음 (BUYER가 등록 시도 등) | "공급사 계정만 가능합니다" 토스트 |
| 404 | 리소스 없음 | 목록으로 복귀 + 안내 |
| 409 | 중복 (이메일·원료코드·중복 주문) | 폼 에러 표시 |
| 500 | 서버 오류 | 공통 에러 모달 + 재시도 |

### (6) 공통 페이징 규약 (목록 API 전체 적용)

요청: `?page=0&size=20&sort=createdAt,desc`

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 137,
    "totalPages": 7,
    "last": false
  }
}
```

---

# ■ Sprint 1 (핵심 가치 / MVP) — 상세 명세

**대상 서비스: user-service, material-service**
**커버 범위: 회원가입·로그인·권한(BUYER/SUPPLIER), 원료의약품 카탈로그 관리, 공급 가능 인증 공장 조회(Ep-01 US1), 여유 생산능력 등록·갱신(Ep-02 US2)**

## 5-1. 인증 (auth-server, OAuth2 Authorization Code Flow)

### 1-1. 로그인 진입 — `GET /oauth2/authorize`

> **axios 호출이 아니라 `window.location.href`로 브라우저를 통째로 이동시킨다.** (CORS 대상 아님)

| 항목 | 내용 |
|---|---|
| Method / URL | `GET http://localhost:8080/oauth2/authorize` |
| 인증 | 불필요 |

**Query Parameter**

| 파라미터 | 값 |
|---|---|
| `response_type` | `code` |
| `client_id` | `web-client` |
| `redirect_uri` | `http://localhost:3000/callback` |
| `scope` | `openid profile read write` |

```js
// src/store/auth.js
function redirectToLogin() {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: import.meta.env.VITE_CLIENT_ID,
    redirect_uri: import.meta.env.VITE_REDIRECT_URI,
    scope: 'openid profile read write'
  })
  window.location.href = `${AUTH_SERVER_URL}/oauth2/authorize?${params}`
}
```

**결과**: 로그인 성공 시 `http://localhost:3000/callback?code=xxxxx` 로 리다이렉트된다.

---

### 1-2. 토큰 교환 — `POST /oauth2/token`

| 항목 | 내용 |
|---|---|
| Method / URL | `POST http://localhost:8080/oauth2/token` |
| Content-Type | `application/x-www-form-urlencoded` ← **JSON 아님, 주의** |
| Authorization | `Basic base64(client_id:client_secret)` |
| 호출 위치 | `/callback` 라우트 진입 시 1회 |

**Request Body (form-urlencoded)**

```
grant_type=authorization_code
&code=2rG5nB...
&redirect_uri=http://localhost:3000/callback
```

**Response 200**

```json
{
  "access_token": "eyJraWQiOiJ...",
  "refresh_token": "AqZ1x...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile read write"
}
```

```js
// src/api/auth.js — 공통 axios 인스턴스가 아니라 순수 axios 사용 (헤더가 다르므로)
exchangeCode(code) {
  const credentials = btoa(`${CLIENT_ID}:${CLIENT_SECRET}`)
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: REDIRECT_URI
  })
  return axios.post(`${API_BASE_URL}/oauth2/token`, body.toString(), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Basic ${credentials}`
    }
  })
}
```

**프론트 후처리**: `access_token`을 `sessionStorage`에 저장 → 즉시 `GET /api/users/me` 호출 → 사용자 정보 저장 → 역할별 홈으로 이동.

---

## 5-2. user-service (`/api/users`)

### 2-1. 회원가입 — `POST /api/users/register`

| 항목 | 내용 |
|---|---|
| Method / URL | `POST /api/users/register` |
| 인증 | **불필요** |
| 화면 | 회원가입 페이지 |

**Request**

```json
{
  "email": "buyer@greenpharm.co.kr",
  "password": "pharm1234",
  "name": "김구매",
  "role": "BUYER",
  "companyName": "그린제약",
  "businessNumber": "123-45-67890"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|---|---|---|---|
| `email` | String | ✔ | 이메일 형식, 중복 불가(409) |
| `password` | String | ✔ | 8자 이상 |
| `name` | String | ✔ | 담당자 이름 |
| `role` | Enum | ✔ | `BUYER`(제약사·연구실) / `SUPPLIER`(공장) |
| `companyName` | String | ✔ | 소속 기관/공장명 |
| `businessNumber` | String | - | 사업자등록번호 |

**Response 201 Created**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 1,
    "email": "buyer@greenpharm.co.kr",
    "name": "김구매",
    "role": "BUYER",
    "companyName": "그린제약",
    "createdAt": "2026-09-03T10:20:31"
  }
}
```

**Response 409 Conflict**

```json
{ "success": false, "message": "이미 사용 중인 이메일입니다", "data": null }
```

> **프론트 처리**: 가입 성공 후 자동 로그인은 하지 않는다(토큰 미발급). "가입 완료 → 로그인 화면으로" 전환한다.

---

### 2-2. 내 정보 조회 — `GET /api/users/me`

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/users/me` |
| 인증 | **필수** (`Authorization: Bearer {token}`) |
| 화면 | 콜백 직후, 헤더 프로필, 마이페이지, 라우터 가드 |

**Request**: Body 없음. `X-User-Id`는 Gateway가 주입하므로 프론트는 아무것도 보내지 않는다.

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 1,
    "email": "buyer@greenpharm.co.kr",
    "name": "김구매",
    "role": "BUYER",
    "companyName": "그린제약",
    "createdAt": "2026-09-03T10:20:31"
  }
}
```

**프론트 활용 (권한 분기의 기준점)**

```js
const isBuyer    = computed(() => user.value?.role === 'BUYER')     // 재고/조달/예측 메뉴
const isSupplier = computed(() => user.value?.role === 'SUPPLIER')  // 원료 등록/생산능력 메뉴
```

---

### 2-3. 사용자 단건 조회 — `GET /api/users/{id}`

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/users/{id}` |
| 인증 | 필수 |
| 화면 | 원료 상세의 "공급사 정보" 표시 |
| Path Param | `id` (Long) |

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 7,
    "email": "sales@koreaapi.com",
    "name": "박공급",
    "role": "SUPPLIER",
    "companyName": "한국API공장",
    "createdAt": "2026-08-11T09:02:10"
  }
}
```

---

### 2-4. (참고) 내부 전용 — 프론트 호출 금지

| Method / URL | 용도 |
|---|---|
| `GET /api/users/internal/{id}` | 서비스 간 호출(Client Credentials). Swagger에는 보이지만 **프론트는 호출하지 않는다.** |

---

## 5-3. material-service (`/api/materials`) — Sprint 1의 핵심

### 공통 Enum 정의

**`category` (원료 분류)**

| 코드 | 표시명 |
|---|---|
| `API_INGREDIENT` | 원료의약품(주성분) |
| `EXCIPIENT` | 부형제 |
| `SOLVENT` | 용매 |
| `REAGENT` | 시약 |
| `INTERMEDIATE` | 중간체 |
| `OTHER` | 기타 |

**`status`**: `ACTIVE`(공급 중) / `INACTIVE`(공급 중단)
**`certifications`**: `GMP`, `DMF`, `ISO9001`, `KGMP` (배열)

---

### 3-1. 원료 목록 조회 / 검색 — `GET /api/materials`

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/materials` |
| 인증 | 필수 |
| 화면 | 원료 카탈로그 목록 (BUYER 메인) |

**Query Parameter**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `keyword` | String | - | 원료명·원료코드·CAS번호 통합 검색 |
| `category` | Enum | - | 카테고리 필터 |
| `certification` | String | - | `GMP` 등 인증 보유 공장만 |
| `minCapacity` | Number | - | 여유 생산능력 이상만 |
| `status` | Enum | `ACTIVE` | 공급 중인 항목만 기본 노출 |
| `page` / `size` | Number | `0` / `20` | 페이징 |
| `sort` | String | `createdAt,desc` | `unitPrice,asc`, `leadTimeDays,asc` 등 |

**요청 예시**

```
GET /api/materials?keyword=세파졸린&category=API_INGREDIENT&minCapacity=500&page=0&size=20
```

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [
      {
        "id": 101,
        "materialCode": "API-CEFA-500",
        "materialName": "세파졸린나트륨",
        "casNumber": "27164-46-1",
        "category": "API_INGREDIENT",
        "unit": "KG",
        "unitPrice": 185000,
        "supplierId": 7,
        "supplierName": "한국API공장",
        "country": "KR",
        "availableCapacity": 1200,
        "leadTimeDays": 21,
        "certifications": ["GMP", "DMF"],
        "status": "ACTIVE",
        "updatedAt": "2026-09-01T14:05:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 37,
    "totalPages": 2,
    "last": false
  }
}
```

```js
// src/api/material.js
getMaterials(params) {
  return api.get('/api/materials', { params })  // params 객체로 넘기면 axios가 쿼리스트링 생성
}
```

---

### 3-2. 원료 상세 조회 — `GET /api/materials/{id}`

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/materials/{id}` |
| 인증 | 필수 |
| 화면 | 원료 상세 페이지 |
| Path Param | `id` (Long) |

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 101,
    "materialCode": "API-CEFA-500",
    "materialName": "세파졸린나트륨",
    "description": "주사제용 세파졸린나트륨 원료. 무균 원료 공정 대응 가능.",
    "casNumber": "27164-46-1",
    "category": "API_INGREDIENT",
    "unit": "KG",
    "unitPrice": 185000,
    "minOrderQuantity": 50,
    "supplierId": 7,
    "supplierName": "한국API공장",
    "country": "KR",
    "availableCapacity": 1200,
    "leadTimeDays": 21,
    "certifications": ["GMP", "DMF"],
    "status": "ACTIVE",
    "createdAt": "2026-08-11T09:10:00",
    "updatedAt": "2026-09-01T14:05:00"
  }
}
```

**Response 404**

```json
{ "success": false, "message": "존재하지 않는 원료입니다: 999", "data": null }
```

---

### 3-3. 【Ep-01 US1】 공급 가능 인증 공장 목록 — `GET /api/materials/code/{materialCode}/suppliers`

> **"전화 수소문 없이 대체 공급처를 즉시 확보한다"** 는 핵심 가치를 담당하는 엔드포인트.
> 동일한 `materialCode`를 공급하는 **여러 공장**을 한 번에 반환한다.

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/materials/code/{materialCode}/suppliers` |
| 인증 | 필수 |
| 화면 | 원료 상세 "대체 공급처" 탭 / 재고 부족 알림의 "공급처 찾기" 버튼 |
| Path Param | `materialCode` (String) — 예: `API-CEFA-500` |

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `requiredQuantity` | Number | 필요 수량. 이 수량 이상 공급 가능한 공장만 필터 |
| `certification` | String | `GMP` 등 특정 인증 보유 공장만 |
| `sort` | String | `leadTimeDays,asc`(기본) / `unitPrice,asc` |

**요청 예시**

```
GET /api/materials/code/API-CEFA-500/suppliers?requiredQuantity=800&certification=GMP&sort=leadTimeDays,asc
```

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "materialCode": "API-CEFA-500",
    "materialName": "세파졸린나트륨",
    "requiredQuantity": 800,
    "suppliers": [
      {
        "materialId": 101,
        "supplierId": 7,
        "supplierName": "한국API공장",
        "country": "KR",
        "availableCapacity": 1200,
        "leadTimeDays": 21,
        "unitPrice": 185000,
        "certifications": ["GMP", "DMF"],
        "suppliable": true
      },
      {
        "materialId": 245,
        "supplierId": 12,
        "supplierName": "Zhejiang Pharma Co.",
        "country": "CN",
        "availableCapacity": 900,
        "leadTimeDays": 45,
        "unitPrice": 142000,
        "certifications": ["GMP"],
        "suppliable": true
      }
    ],
    "totalCount": 2
  }
}
```

**Response 200 (공급 가능 공장 없음)**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "materialCode": "API-CEFA-500",
    "requiredQuantity": 800,
    "suppliers": [],
    "totalCount": 0
  }
}
```

> **프론트 처리**: `totalCount === 0`은 **에러가 아니라 정상 응답**이다. 빈 배열일 때 "현재 조건을 만족하는 인증 공장이 없습니다. 필요 수량을 조정해 보세요" 라는 Empty State를 렌더링하고, 수량 필터를 낮추는 버튼을 제공한다.

---

### 3-4. 【Ep-02 US2】 원료 등록 — `POST /api/materials`

| 항목 | 내용 |
|---|---|
| Method / URL | `POST /api/materials` |
| 인증 | 필수 + **`SUPPLIER` 권한** |
| 화면 | 공급사 마이페이지 → "공급 품목 등록" |

> `supplierId`는 **Body에 넣지 않는다.** Gateway가 주입한 `X-User-Id`로 백엔드가 자동 설정한다.

**Request**

```json
{
  "materialCode": "API-CEFA-500",
  "materialName": "세파졸린나트륨",
  "description": "주사제용 세파졸린나트륨 원료",
  "casNumber": "27164-46-1",
  "category": "API_INGREDIENT",
  "unit": "KG",
  "unitPrice": 185000,
  "minOrderQuantity": 50,
  "availableCapacity": 1200,
  "leadTimeDays": 21,
  "certifications": ["GMP", "DMF"],
  "country": "KR"
}
```

| 필드 | 타입 | 필수 | 검증 규칙 |
|---|---|---|---|
| `materialCode` | String | ✔ | 공급사 내 중복 불가(409) |
| `materialName` | String | ✔ | 공백 불가 |
| `category` | Enum | ✔ | 위 Enum 표 참조 |
| `unit` | String | ✔ | `KG` / `L` / `EA` |
| `unitPrice` | Number | ✔ | 0 이상 |
| `availableCapacity` | Number | ✔ | 0 이상 (여유 생산능력) |
| `leadTimeDays` | Number | ✔ | 1 이상 |
| `certifications` | String[] | - | 미입력 시 빈 배열 |

**Response 201 Created**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 101,
    "materialCode": "API-CEFA-500",
    "materialName": "세파졸린나트륨",
    "supplierId": 7,
    "availableCapacity": 1200,
    "status": "ACTIVE",
    "createdAt": "2026-09-03T11:02:44"
  }
}
```

**Response 403 Forbidden** — BUYER 계정이 호출한 경우

```json
{ "success": false, "message": "공급사(SUPPLIER)만 원료를 등록할 수 있습니다", "data": null }
```

> **프론트 처리**: 애초에 `isSupplier`가 false면 등록 메뉴/버튼을 렌더링하지 않는다(1차 방어). 그래도 403이 오면 토스트로 안내한다(2차 방어).

---

### 3-5. 원료 정보 수정 — `PUT /api/materials/{id}`

| 항목 | 내용 |
|---|---|
| Method / URL | `PUT /api/materials/{id}` |
| 인증 | 필수 + `SUPPLIER` + **본인 소유 품목만** |
| 화면 | 공급사 마이페이지 → 품목 수정 |

**Request** (등록과 동일 스키마, 전체 필드 전송)

```json
{
  "materialName": "세파졸린나트륨",
  "description": "주사제용 세파졸린나트륨 원료 (2026 공정 개선)",
  "category": "API_INGREDIENT",
  "unit": "KG",
  "unitPrice": 179000,
  "minOrderQuantity": 50,
  "availableCapacity": 900,
  "leadTimeDays": 18,
  "certifications": ["GMP", "DMF", "ISO9001"],
  "status": "ACTIVE"
}
```

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 101,
    "materialCode": "API-CEFA-500",
    "unitPrice": 179000,
    "availableCapacity": 900,
    "leadTimeDays": 18,
    "status": "ACTIVE",
    "updatedAt": "2026-09-03T11:30:12"
  }
}
```

**Response 403** — 다른 공급사의 품목을 수정 시도한 경우

```json
{ "success": false, "message": "본인이 등록한 원료만 수정할 수 있습니다", "data": null }
```

---

### 3-6. 【Ep-02 US2】 여유 생산능력만 갱신 — `PATCH /api/materials/{id}/capacity`

> 공장은 **생산능력만 자주 바꾼다.** 전체 폼을 다시 채우게 하면 UX가 나빠지므로 부분 갱신 전용 API를 둔다.
> 목록 화면에서 인라인 편집(숫자만 고치고 Enter)으로 처리한다.

| 항목 | 내용 |
|---|---|
| Method / URL | `PATCH /api/materials/{id}/capacity` |
| 인증 | 필수 + `SUPPLIER` + 본인 소유 |
| 화면 | 공급사 마이페이지 품목 목록 인라인 편집 |

**Request**

```json
{ "availableCapacity": 750 }
```

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "id": 101,
    "materialCode": "API-CEFA-500",
    "availableCapacity": 750,
    "updatedAt": "2026-09-03T11:41:07"
  }
}
```

---

### 3-7. 내 공급 품목 목록 — `GET /api/materials/my`

| 항목 | 내용 |
|---|---|
| Method / URL | `GET /api/materials/my` |
| 인증 | 필수 + `SUPPLIER` |
| 화면 | 공급사 마이페이지 |

> `X-User-Id` 기반으로 본인 품목만 반환. **경로에 supplierId를 넣지 않는다.**

**Query Parameter**: `status`, `page`, `size`

**Response 200**

```json
{
  "success": true,
  "message": "성공",
  "data": {
    "content": [
      {
        "id": 101,
        "materialCode": "API-CEFA-500",
        "materialName": "세파졸린나트륨",
        "category": "API_INGREDIENT",
        "unitPrice": 179000,
        "availableCapacity": 750,
        "leadTimeDays": 18,
        "status": "ACTIVE",
        "updatedAt": "2026-09-03T11:41:07"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 3-8. 공급 중단 처리 — `DELETE /api/materials/{id}`

| 항목 | 내용 |
|---|---|
| Method / URL | `DELETE /api/materials/{id}` |
| 인증 | 필수 + `SUPPLIER` + 본인 소유 |
| 동작 | 물리 삭제가 아니라 `status = INACTIVE` 전환(Soft Delete) |

**Response 200**

```json
{ "success": true, "message": "공급이 중단되었습니다", "data": null }
```

> **프론트 처리**: 확인 모달 후 호출, 성공 시 목록에서 상태 배지를 "공급 중단"으로 변경한다.

---

### 3-9. (참고) 내부 전용 — 프론트 호출 금지

| Method / URL | 호출 주체 | 용도 |
|---|---|---|
| `GET /api/materials/internal/{id}` | order-service | 조달 주문 생성 시 원료 검증·단가 조회 |
| `GET /api/materials/internal/exists/{id}` | order-service | 원료 존재 확인 |
| `POST /api/materials/internal/{id}/capacity-deduct` | order-service | 결제 완료 후 생산능력 차감 |
| `GET /api/materials/internal/candidates` | recommend-service | 추천 후보 공장 조회 |

---

# ■ Sprint 2 (확장 기능) — 기능별 End-to-End 흐름

> Sprint 2는 상세 스키마 대신 **기능 1개당 End-to-End 흐름 + 대표 엔드포인트**만 정의한다.
> (Sprint 1 완료 후 Swagger 확인하며 상세 스키마를 확정한다.)

## E2E-1. 원료 재고 등록 및 부족 임계치 감지 알림 (order-service)

**흐름**
```
[재고 관리 화면] BUYER가 보유 원료·현재고·임계치 입력
   → POST /api/inventories                                  (재고 등록)
   → order-service DB 저장
   → [스케줄러] 주기 실행: currentStock < threshold 인 항목 감지
   → 부족 알림 레코드 생성
[대시보드] 진입 시 GET /api/inventories/alerts               (부족 알림 조회)
   → 알림 카드의 "공급처 찾기" 클릭
   → GET /api/materials/code/{materialCode}/suppliers        (Sprint 1 API 재사용)
```

**대표 엔드포인트**

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/inventories` | 보유 원료·재고량·임계치 등록 |
| `GET` | `/api/inventories/my` | 내 재고 목록 (재고율 포함) |
| `PATCH` | `/api/inventories/{id}/stock` | 현재고 수정 |
| `GET` | `/api/inventories/alerts` | 임계치 미만 부족 알림 목록 |

```json
// GET /api/inventories/alerts → data 예시
{
  "alerts": [
    {
      "inventoryId": 55,
      "materialCode": "API-CEFA-500",
      "materialName": "세파졸린나트륨",
      "currentStock": 120,
      "threshold": 500,
      "shortageQuantity": 380,
      "severity": "CRITICAL",
      "detectedAt": "2026-09-03T09:00:00"
    }
  ]
}
```

---

## E2E-2. 조달 신청 → 결제 (order-service ↔ payment-service, 동기 REST)

**흐름**
```
[공급 가능 공장 목록] "조달 신청" 클릭
   → POST /api/orders  { materialId, quantity }
   → order-service: 원료 검증(material-service 내부 호출) → 주문 PENDING 생성
   → order-service → payment-service 내부 REST 동기 호출 (결제 요청)
   → 응답: 주문 ID + 결제 상태
[주문 완료 화면] GET /api/orders/{id} 로 상태 표시
```

**대표 엔드포인트**

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/orders` | 조달 신청 (주문 생성 + 결제 요청) |
| `GET` | `/api/orders/my` | 내 주문 목록 |
| `GET` | `/api/payments/my` | 내 결제 내역 |

```json
// POST /api/orders 요청 / 응답
{ "materialId": 101, "quantity": 800, "requiredDate": "2026-10-15" }
```
```json
{ "orderId": 3001, "status": "PENDING", "paymentId": 9001, "totalAmount": 143200000 }
```

---

## E2E-3. 【Ep-01 US2】 주문 상태 실시간 반영 (Kafka `payment.completed`)

**흐름**
```
payment-service 결제 완료
   → Kafka publish: payment.completed
   → order-service 구독 → 주문 상태 PENDING → CONFIRMED 전환
   → material-service 내부 호출로 availableCapacity 차감
[공장 주문 관리 화면] 폴링(5초) 또는 재조회로 상태 갱신
   → GET /api/orders/supplier?status=CONFIRMED
```

**대표 엔드포인트**

| Method | URL | 설명 |
|---|---|---|
| `GET` | `/api/orders/{id}/status` | 주문 상태 단건 조회 (폴링용 경량 응답) |
| `GET` | `/api/orders/supplier` | 공장이 받은 주문 목록 (상태 필터) |

```json
// GET /api/orders/{id}/status
{ "orderId": 3001, "status": "CONFIRMED", "updatedAt": "2026-09-03T12:00:04" }
```

> **프론트 처리**: 상태가 `PENDING`인 동안만 5초 간격 폴링, `CONFIRMED`/`FAILED` 도달 시 폴링 중단.

---

## E2E-4. 【Ep-03 US1】 AI 수요 예측 (recommend-service)

**흐름**
```
[수요 예측 화면] 원료 선택 + 예측 기간 선택
   → GET /api/forecast/demand?materialCode=API-CEFA-500&horizon=90
   → recommend-service: 공공데이터(질병청·기상청) 지표 수집
     + 시계열 모델(Prophet/LSTM) 추론
   → 예측량 + 신뢰구간 반환
[화면] 라인 차트 + 신뢰도 배지 렌더링
```

**대표 엔드포인트**

| Method | URL | 설명 |
|---|---|---|
| `GET` | `/api/forecast/demand` | 원료별 수요 예측 (예측량·신뢰도) |

```json
// data 예시
{
  "materialCode": "API-CEFA-500",
  "horizonDays": 90,
  "confidence": 0.87,
  "modelType": "PROPHET",
  "points": [
    { "date": "2026-10-01", "predicted": 820, "lower": 730, "upper": 910 }
  ],
  "externalFactors": ["INFLUENZA_SEASON", "TEMPERATURE_DROP"]
}
```

> **주의**: FastAPI 서비스라 응답에 `success/message/data` 래퍼가 없을 수 있다. **응답 포맷을 백엔드와 먼저 합의**하고, 다르면 프론트 언랩 헬퍼에서 분기 처리한다.

---

## E2E-5. 공급 리스크 점수 기반 공장 추천 (recommend-service)

**흐름**
```
[부족 알림 / 원료 상세] "AI 추천 공장 보기" 클릭
   → GET /api/recommend/suppliers?materialCode=API-CEFA-500&quantity=800
   → recommend-service: material-service 후보 조회
     + 리드타임·가격·인증·과거 납기 준수율로 리스크 점수 산출
   → 점수 내림차순 추천 목록 반환
[화면] 추천 사유 배지와 함께 카드 렌더링 → "조달 신청"으로 연결(E2E-2)
```

**대표 엔드포인트**

| Method | URL | 설명 |
|---|---|---|
| `GET` | `/api/recommend/suppliers` | 리스크 점수 기반 공장 추천 |

```json
{
  "materialCode": "API-CEFA-500",
  "recommendations": [
    {
      "supplierId": 7,
      "supplierName": "한국API공장",
      "riskScore": 12.5,
      "grade": "LOW_RISK",
      "reasons": ["리드타임 21일로 최단", "GMP·DMF 인증 보유", "납기 준수율 98%"]
    }
  ]
}
```

---

# ■ 프론트엔드 구현 가이드

## (1) API 레이어 파일 구조

```
src/api/
 ├─ index.js       # axios 인스턴스 + 인터셉터 (토큰 주입, 401 처리)
 ├─ auth.js        # /oauth2/token, /api/users/me, /api/users/register   ← Sprint 1
 ├─ material.js    # /api/materials/*                                     ← Sprint 1
 ├─ inventory.js   # /api/inventories/*                                   ← Sprint 2
 ├─ order.js       # /api/orders/*, /api/payments/*                       ← Sprint 2
 └─ recommend.js   # /api/forecast/*, /api/recommend/*                    ← Sprint 2
```

## (2) 공통 axios 인스턴스

```js
// src/api/index.js
import axios from 'axios'
import { useAuthStore } from '@/store/auth.js'

const api = axios.create({
  baseURL: '',                 // Vite proxy가 8080으로 전달하므로 비워둔다
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 요청: 모든 호출에 Bearer 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) config.headers.Authorization = `Bearer ${auth.accessToken}`
  return config
})

// 응답: 401 → 토큰 폐기 후 로그인, 403 → 권한 안내
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    if (status === 401) { useAuthStore().logout(); window.location.href = '/login' }
    if (status === 403) alert(err.response?.data?.message ?? '권한이 없습니다')
    return Promise.reject(err)
  }
)

export default api
```

## (3) Sprint 1 화면별 호출 시퀀스

| 화면 | 호출 순서 |
|---|---|
| 회원가입 | `POST /api/users/register` → 로그인 화면 전환 |
| 로그인 | `window.location → GET /oauth2/authorize` |
| `/callback` | `POST /oauth2/token` → `GET /api/users/me` → 역할별 홈 |
| 원료 카탈로그 | `GET /api/materials?page=0&size=20` (필터 변경 시 재호출) |
| 원료 상세 | `GET /api/materials/{id}` → (탭 전환 시) `GET /api/materials/code/{code}/suppliers` |
| 공급처 찾기 | `GET /api/materials/code/{code}/suppliers?requiredQuantity=N` |
| 공급사 마이페이지 | `GET /api/materials/my` → 인라인 편집 시 `PATCH /api/materials/{id}/capacity` |
| 품목 등록 | `POST /api/materials` → 성공 시 `/my`로 이동 |
| 품목 수정 | `GET /api/materials/{id}` → `PUT /api/materials/{id}` |

## (4) 백엔드에 요청할 수정 사항 체크리스트 (Sprint 1)

- [ ] `User.Role` Enum을 `BUYER` / `SUPPLIER`로 정의, `companyName`·`businessNumber` 필드 추가
- [ ] `Material` 엔티티: `materialCode`, `casNumber`, `unit`, `availableCapacity`, `leadTimeDays`, `certifications`, `country` 추가
- [ ] 목록 API 응답을 **페이징 래퍼(`content`/`totalElements`)** 로 통일 (프론트 무한스크롤·페이저 대응)
- [ ] `GET /api/materials/code/{materialCode}/suppliers` 신규 구현 (Ep-01 US1 핵심)
- [ ] `PATCH /api/materials/{id}/capacity` 신규 구현 (Ep-02 US2 인라인 편집)
- [ ] `GET /api/materials/my` 신규 구현 (`X-User-Id` 기준, 경로에 supplierId 노출 금지)
- [ ] 권한 검증: 등록/수정/삭제는 `SUPPLIER` + 본인 소유만 → 위반 시 **403 + 한글 message**
- [ ] Gateway 라우팅에 `/api/materials/**` → `material-service` 추가
- [ ] 모든 에러 응답을 `{ success:false, message, data:null }` 포맷으로 통일

## (5) Swagger UI로 명세 검증하는 순서

1. `docker-compose up -d` 로 전체 기동
2. `http://localhost:8761` (Eureka)에서 서비스 등록 확인
3. `http://localhost:8082/swagger-ui/index.html` 에서 material-service 스키마 확인
4. Swagger의 **Schemas** 섹션에서 필드명·타입을 그대로 복사해 프론트 타입/폼과 일치시킴
5. 실제 호출은 반드시 **Gateway(8080)** 를 통해 테스트 (토큰·`X-User-Id` 주입 경로 검증)
