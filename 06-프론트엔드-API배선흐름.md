# 6. 프론트엔드 API 호출 배선 흐름

> 구현 완료된 `msa-MyService/vue-frontend` 기준.
> 모든 다이어그램은 mermaid-cli로 렌더링 검증을 마쳤으며, 원본 `.mmd`와 `.png`는 `06-배선흐름-images/`에 있다.

---

## 1. 레이어 배선 — 화면부터 마이크로서비스까지

![레이어 배선](06-배선흐름-images/01-layers.png)

프론트엔드는 3개 계층으로 분리되어 있고, 각 계층은 아래 계층만 호출한다.

| 계층 | 위치 | 책임 | 디자인 변경 영향 |
|---|---|---|---|
| 1. 화면 | `views/*.vue` | 마크업과 렌더링만 | **교체 대상** |
| 2. 상태 | `store/auth.js`, `composables/useAsync.js` | 토큰·역할, loading/error/data | 영향 없음 |
| 3. API | `api/*.js` | HTTP 호출. **URL은 여기에만 존재** | 영향 없음 |

`api/index.js`(axios 인스턴스)가 세 가지를 가로챈다.

1. **요청**: `store/auth.js`의 `accessToken`을 `Authorization: Bearer`로 자동 첨부
2. **응답**: `{ success, message, data }` 래퍼에서 `data`만 꺼내 반환(`unwrap`)
3. **401**: 토큰을 폐기하고 `/login`으로 이동

> `X-User-Id`는 프론트가 보내지 않는다. API Gateway가 JWT를 검증한 뒤 백엔드로 주입한다.
> 그래서 "내 것" 조회는 경로에 id를 넣지 않고 `/me`, `/my`를 쓴다.

<details>
<summary>mermaid 원본</summary>

```mermaid
flowchart TB
    subgraph L1["1. 화면 계층 · .vue — 디자인 교체 대상"]
        direction LR
        V_AUTH["LoginView<br/>CallbackView<br/>MyPageView"]
        V_MAT["material/<br/>4개 화면"]
        V_INV["inventory/<br/>2개 화면"]
        V_ORD["order/<br/>3개 화면"]
        V_PAY["payment/<br/>1개 화면"]
        V_REC["recommend/<br/>1개 화면"]
    end

    subgraph L2["2. 상태 계층 — 디자인과 무관, 재사용됨"]
        direction LR
        STORE["store/auth.js<br/>토큰 · 역할 BUYER SUPPLIER"]
        ASYNC["composables/useAsync.js<br/>loading · error · data"]
    end

    subgraph L3["3. API 계층 · api/*.js — URL은 여기에만 존재"]
        direction LR
        A_USER["user.js"]
        A_MAT["material.js"]
        A_INV["inventory.js"]
        A_ORD["order.js"]
        A_PAY["payment.js"]
        A_REC["recommend.js"]
    end

    AXIOS["api/index.js<br/>axios 인스턴스<br/>Authorization 자동 첨부 · 401 처리 · unwrap"]
    PROXY["Vite proxy<br/>localhost 3000"]
    GW["API Gateway 8080<br/>JWT 검증 후 X-User-Id 주입"]

    subgraph BE["백엔드 마이크로서비스"]
        direction LR
        S_USER["user-service<br/>8081"]
        S_MAT["material-service<br/>8082"]
        S_ORD["order-service<br/>8083"]
        S_PAY["payment-service<br/>8084"]
        S_REC["recommend-service<br/>8085"]
    end

    V_AUTH --> STORE
    V_MAT --> ASYNC
    V_INV --> ASYNC
    V_ORD --> ASYNC
    V_PAY --> ASYNC
    V_REC --> ASYNC

    STORE --> A_USER
    ASYNC --> A_MAT
    ASYNC --> A_INV
    ASYNC --> A_ORD
    ASYNC --> A_PAY
    ASYNC --> A_REC

    A_USER --> AXIOS
    A_MAT --> AXIOS
    A_INV --> AXIOS
    A_ORD --> AXIOS
    A_PAY --> AXIOS
    A_REC --> AXIOS

    STORE -. "accessToken 주입" .-> AXIOS
    AXIOS -. "401 발생 시 logout" .-> STORE

    AXIOS --> PROXY --> GW
    GW --> S_USER
    GW --> S_MAT
    GW --> S_ORD
    GW --> S_PAY
    GW --> S_REC
```

</details>

---

## 2. 로그인 배선 — OAuth2 Authorization Code Flow

![로그인 흐름](06-배선흐름-images/02-login.png)

**핵심 지점 3가지**

- **`GET /oauth2/authorize`는 axios 호출이 아니다.** `window.location.href`로 브라우저를 통째로 이동시킨다(CORS 대상 아님).
- **`POST /oauth2/token`은 공통 axios 인스턴스를 쓰지 않는다.** `Content-Type`이 `application/x-www-form-urlencoded`이고 인증이 `Basic`이라 형식이 다르다.
- **`GET /api/users/me`의 응답이 역할 분기의 기준점이다.** 여기서 받은 `role`로 `isBuyer`/`isSupplier`가 결정되고, 헤더 메뉴·라우터 가드·버튼 노출이 모두 여기에 연결된다.

<details>
<summary>mermaid 원본</summary>

```mermaid
sequenceDiagram
    autonumber
    actor U as 사용자
    participant LV as LoginView.vue
    participant AS as store/auth.js
    participant AUTH as auth-server 9000
    participant CV as CallbackView.vue
    participant UA as api/user.js
    participant AX as api/index.js
    participant GW as API Gateway 8080
    participant US as user-service 8081

    Note over U,US: 회원가입 — POST /api/users/register
    U->>LV: 회원가입 폼 제출
    LV->>AS: register 호출
    AS->>UA: userApi.register
    UA->>AX: POST /api/users/register
    AX->>GW: 인증 헤더 없이 전송
    GW->>US: 라우팅
    US-->>LV: 201 Created 후 로그인 화면 전환

    Note over U,US: 로그인 — OAuth2 Authorization Code Flow
    U->>LV: 로그인 버튼
    LV->>AS: redirectToLogin
    AS->>AUTH: GET /oauth2/authorize 브라우저 이동
    AUTH-->>CV: /callback?code=xxx 리다이렉트
    CV->>AS: handleCallback with code
    AS->>AUTH: POST /oauth2/token
    AUTH-->>AS: access_token
    AS->>AS: sessionStorage 저장

    Note over U,US: 내 정보 — GET /api/users/me
    AS->>UA: userApi.getMe
    UA->>AX: GET /api/users/me
    AX->>AX: Authorization Bearer 자동 첨부
    AX->>GW: 요청 전달
    GW->>GW: JWT 검증 후 X-User-Id 주입
    GW->>US: GET /api/users/me
    US-->>AX: success message data 래퍼
    AX-->>AS: unwrap 후 data 반환
    AS->>AS: isBuyer / isSupplier 결정
    AS-->>U: 역할별 홈으로 이동
```

</details>

---

## 3. 화면 → API 함수 → 엔드포인트 매핑

![화면 엔드포인트 매핑](06-배선흐름-images/03-mapping.png)

| 담당 | 화면 | API 함수 | 엔드포인트 |
|---|---|---|---|
| **본인** | LoginView | `userApi.register` | `POST /api/users/register` |
| **본인** | CallbackView | `authApi.exchangeCode` | `POST /oauth2/token` |
| **본인** | MyPageView | `userApi.getMe` / `getById` | `GET /api/users/me` / `GET /api/users/{id}` |
| material | MaterialListView | `materialApi.list` | `GET /api/materials` |
| material | MaterialDetailView | `materialApi.get` / `suppliersByCode` | `GET /api/materials/{id}` / `GET /api/materials/code/{code}/suppliers` |
| material | MaterialFormView | `materialApi.create` / `update` | `POST /api/materials` / `PUT /api/materials/{id}` |
| material | SupplierMaterialsView | `materialApi.my` / `updateCapacity` / `remove` | `GET /api/materials/my` / `PATCH /api/materials/{id}/capacity` / `DELETE /api/materials/{id}` |
| inventory | InventoryListView | `inventoryApi.create` / `my` / `updateStock` | `POST /api/inventories` / `GET /api/inventories/my` / `PATCH /api/inventories/{id}/stock` |
| inventory | InventoryAlertView | `inventoryApi.alerts` | `GET /api/inventories/alerts` |
| order | OrderListView | `orderApi.create` / `my` | `POST /api/orders` / `GET /api/orders/my` |
| order | OrderDetailView | `orderApi.get` / `getStatus` | `GET /api/orders/{id}` / `GET /api/orders/{id}/status` |
| order | SupplierOrderView | `orderApi.supplier` | `GET /api/orders/supplier` |
| payment | PaymentListView | `paymentApi.create` / `my` | `POST /api/payments` / `GET /api/payments/my` |
| recommend | RecommendView | `recommendApi.get` | `GET /api/recommend` |

<details>
<summary>mermaid 원본</summary>

```mermaid
flowchart LR
    subgraph OWN["담당 · 프론트엔드 본인"]
        MY1["LoginView"] --> MF1["userApi.register"] --> ME1["POST /api/users/register"]
        MY2["CallbackView"] --> MF2["authApi.exchangeCode"] --> ME2["POST /oauth2/token"]
        MY3["MyPageView"] --> MF3["userApi.getMe<br/>userApi.getById"] --> ME3["GET /api/users/me<br/>GET /api/users/id"]
    end

    subgraph MAT["담당 · material 팀원"]
        T1["MaterialListView"] --> F1["materialApi.list"] --> E1["GET /api/materials"]
        T2["MaterialDetailView"] --> F2["materialApi.get<br/>materialApi.suppliersByCode"] --> E2["GET /api/materials/id<br/>GET /api/materials/code/.../suppliers"]
        T3["MaterialFormView"] --> F3["materialApi.create<br/>materialApi.update"] --> E3["POST /api/materials<br/>PUT /api/materials/id"]
        T4["SupplierMaterialsView"] --> F4["materialApi.my<br/>updateCapacity<br/>remove"] --> E4["GET /api/materials/my<br/>PATCH /api/materials/id/capacity<br/>DELETE /api/materials/id"]
    end

    subgraph INV["담당 · inventory 팀원"]
        T5["InventoryListView"] --> F5["inventoryApi.create<br/>my · updateStock"] --> E5["POST /api/inventories<br/>GET /api/inventories/my<br/>PATCH /api/inventories/id/stock"]
        T6["InventoryAlertView"] --> F6["inventoryApi.alerts"] --> E6["GET /api/inventories/alerts"]
    end

    subgraph ORD["담당 · order 팀원"]
        T7["OrderListView"] --> F7["orderApi.create · my"] --> E7["POST /api/orders<br/>GET /api/orders/my"]
        T8["OrderDetailView"] --> F8["orderApi.get<br/>orderApi.getStatus"] --> E8["GET /api/orders/id<br/>GET /api/orders/id/status"]
        T9["SupplierOrderView"] --> F9["orderApi.supplier"] --> E9["GET /api/orders/supplier"]
    end

    subgraph PAY["담당 · payment 팀원"]
        T10["PaymentListView"] --> F10["paymentApi.create · my"] --> E10["POST /api/payments<br/>GET /api/payments/my"]
    end

    subgraph REC["담당 · recommend 팀원"]
        T11["RecommendView"] --> F11["recommendApi.get"] --> E11["GET /api/recommend"]
    end
```

</details>

---

## 4. 핵심 시나리오 배선 — 재고 부족부터 수주 확인까지

![핵심 시나리오](06-배선흐름-images/04-scenario.png)

화면 간 값 전달은 **라우터 query**로 한다. 전역 상태를 만들지 않아, 화면을 재배치해도 배선이 끊기지 않는다.

- 부족 알림 → 추천: `router.push('/recommend?materialCode=...&quantity=...')`
- 공급처 → 조달 신청: `router.push('/orders?materialId=...&quantity=...')`

**Sprint 경계**: Sprint 1에서는 주문이 `PENDING`에서 멈춘다. 결제(Sprint 2)가 붙어
`payment.completed` 이벤트가 발행되어야 `CONFIRMED`로 전환된다.
`OrderDetailView`는 `PENDING`인 동안에만 5초 간격으로 상태를 폴링하고, 종료 상태에 도달하면 폴링을 멈춘다.

<details>
<summary>mermaid 원본</summary>

```mermaid
flowchart TB
    START(["재고 부족 발생"]) --> A1["InventoryAlertView<br/>GET /api/inventories/alerts"]
    A1 -->|"공급처 찾기 클릭<br/>query materialCode quantity 전달"| A2["RecommendView<br/>GET /api/recommend"]
    A1 -.->|"원료 상세에서도 진입 가능"| B2["MaterialDetailView<br/>GET /api/materials/code/.../suppliers"]

    A2 --> A3{"공급 가능 공장<br/>존재 여부"}
    B2 --> A3
    A3 -->|"없음 · totalCount 0"| A4["빈 상태 안내<br/>필요 수량 조정 유도"]
    A3 -->|"있음"| A5["조달 신청 버튼<br/>router.push /orders"]

    A5 --> A6["OrderListView<br/>POST /api/orders"]
    A6 --> A7["주문 PENDING 생성<br/>Sprint 1은 여기서 종료"]

    A7 --> A8["PaymentListView<br/>POST /api/payments"]
    A8 --> A9["payment-service<br/>Kafka payment.completed 발행"]
    A9 --> A10["order-service 구독<br/>주문 CONFIRMED 전환"]
    A10 --> A11["material-service<br/>availableCapacity 차감"]

    A7 --> A12["OrderDetailView<br/>GET /api/orders/id/status<br/>5초 폴링"]
    A10 -.->|"상태 변경 감지"| A12
    A12 --> A13(["CONFIRMED 도달 시 폴링 중단"])

    A11 --> A14["SupplierOrderView<br/>GET /api/orders/supplier"]
```

</details>

---

## 5. 배선 규약 요약

| 규약 | 이유 |
|---|---|
| HTTP 호출은 `api/`에만 둔다 | 화면이 URL을 모르므로, 엔드포인트가 바뀌어도 화면은 그대로다 |
| 비동기 상태는 `useAsync`로 감싼다 | `run()`은 예외를 던지지 않고 `{ ok }`를 반환해, 화면마다 try/catch를 반복하지 않는다 |
| 응답 언랩은 api 모듈에서 끝낸다 | 화면에서 `res.data.data`를 파지 않는다 |
| 권한 분기는 `auth` 스토어만 본다 | 역할 판단이 한 곳에 있어 메뉴·가드·버튼이 함께 움직인다 |
| 컴포넌트에 `scoped style`을 쓰지 않는다 | 디자인 교체 시 `global.css`만 바꾸면 된다 |
| 빈 결과는 에러가 아니다 | `totalCount === 0`은 정상 응답이므로 `.empty` 안내를 렌더링한다 |
