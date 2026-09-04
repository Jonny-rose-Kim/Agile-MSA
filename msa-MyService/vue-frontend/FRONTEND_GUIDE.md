# 프론트엔드 개발 가이드 (팀 공유용)

## 1. 실행 방법

```bash
cd msa-MyService/vue-frontend
npm install          # 최초 1회
npm run dev          # http://localhost:3000
```

- 모든 API 요청은 **Vite proxy → API Gateway(8080)** 로 전달된다. 개별 서비스 포트를 직접 호출하지 않는다.
- 백엔드가 아직 강의 도메인이면 로그인 외 화면은 404/에러 상태로 보인다. **정상이다.**
  각 담당자가 백엔드를 개편하면 해당 화면부터 순차적으로 살아난다.

## 2. 담당 분배

| 담당 API | 담당자 | 수정할 파일 |
|---|---|---|
| `/api/users`, 로그인 전체 | **프론트엔드(본인)** | `api/user.js`, `api/auth.js`, `store/auth.js`, `views/LoginView.vue`, `views/CallbackView.vue`, `views/MyPageView.vue` |
| `/api/materials` | 팀원 | `api/material.js`, `views/material/*.vue` |
| `/api/inventories` | 팀원 | `api/inventory.js`, `views/inventory/*.vue` |
| `/api/orders` | 팀원 | `api/order.js`, `views/order/*.vue` |
| `/api/payments` | 팀원 | `api/payment.js`, `views/payment/*.vue` |
| `/api/recommend` | 팀원 | `api/recommend.js`, `views/recommend/*.vue` |

**담당 폴더 밖의 파일은 건드리지 않는다.** 공통 파일(`api/index.js`, `store/auth.js`, `router/index.js`,
`composables/useAsync.js`, `assets/styles/global.css`)을 바꿔야 하면 프론트엔드 담당자에게 요청한다.

## 3. 폴더 구조와 역할

```
src/
├─ api/                 # HTTP 호출만. UI 코드 없음
│  ├─ index.js          # [공통] axios 인스턴스 + 토큰 주입 + 401 처리 + unwrap/errorMessage
│  ├─ auth.js           # [공통] OAuth2 토큰 교환
│  ├─ user.js           # /api/users
│  ├─ material.js       # /api/materials  + 카테고리·인증·단위 상수
│  ├─ inventory.js      # /api/inventories
│  ├─ order.js          # /api/orders
│  ├─ payment.js        # /api/payments
│  └─ recommend.js      # /api/recommend
├─ composables/
│  └─ useAsync.js       # [공통] loading / error / data 상태 관리
├─ store/auth.js        # [공통] 로그인 상태, 역할(BUYER/SUPPLIER) 판단
├─ router/index.js      # [공통] 라우트 + 권한 가드
├─ views/               # 화면. 담당 폴더별로 분리
└─ assets/styles/global.css   # [공통] 임시 스타일. 디자인 확정 시 이 파일만 교체
```

## 4. 왜 이렇게 나눴나 — 디자인을 나중에 바꿔도 기능이 유지되도록

1. **HTTP 호출은 `api/`에만 있다.** 화면은 `materialApi.list()`만 부르고 URL을 모른다.
2. **상태 관리는 `useAsync`에만 있다.** 화면은 `loading / error / data`를 렌더링만 한다.
3. **컴포넌트에 `scoped style`을 쓰지 않는다.** 스타일은 전부 `global.css`의 클래스로만 준다.

→ 디자인 교체 시 **`.vue`의 `<template>` 부분과 `global.css`만** 갈아끼우면 되고,
   `<script setup>`의 API 호출·상태 로직은 그대로 재사용된다.

## 5. 화면을 구현할 때 지켜야 할 규약

### (1) `X-User-Id`를 절대 직접 보내지 않는다
Gateway가 JWT에서 추출해 주입한다. 프론트가 보내면 위조이고, 401이 난다.
그래서 "내 것" 조회는 경로에 id를 넣지 않고 `/me`, `/my`를 쓴다.

### (2) 응답 언랩은 api 모듈에서 이미 끝난다
```js
// O — api 모듈이 { success, message, data }에서 data를 꺼내 돌려준다
const page = await materialApi.list({ page: 0 })
console.log(page.content)

// X — 화면에서 res.data.data를 파지 않는다
```

### (3) 비동기 호출은 `useAsync`로 감싼다
```js
import { useAsync } from '@/composables/useAsync.js'
import { materialApi } from '@/api/material.js'

const list = useAsync(materialApi.list, { initial: { content: [] } })
await list.run({ page: 0, size: 20 })
// 템플릿에서: list.loading.value / list.error.value / list.data.value
```
`run()`은 **예외를 던지지 않는다.** 성공 여부는 반환값 `{ ok }`로 판단한다.

### (4) 권한 분기는 `auth` 스토어만 본다
```js
const auth = useAuthStore()
auth.isBuyer      // 제약사·연구실
auth.isSupplier   // 공급 공장
```
라우트 접근 제한은 `router/index.js`의 `meta.roles`로 선언한다.
```js
{ path: '/inventories', meta: { requiresAuth: true, roles: ['BUYER'] } }
```

### (5) 빈 결과는 에러가 아니다
`totalCount === 0`, 빈 배열은 정상 응답이다. `.empty` 클래스로 안내 문구를 렌더링한다.

## 6. 디자인 시스템 — 마크업만 바꿔 화면을 만든다

모든 스타일은 `src/assets/styles/global.css` **한 파일**에 있다.
`.vue` 파일에는 `<style scoped>`를 **두지 않는다.** 색·간격·모서리를 바꾸려면
그 파일 맨 위 `:root` 토큰만 고치면 앱 전체가 따라 바뀐다.

### 화면 한 장의 뼈대

```html
<div class="page-head">
  <div class="page-head__text">
    <h1 class="page-title">화면 제목</h1>
    <p class="page-desc">한 줄 설명</p>
  </div>
  <div class="page-head__actions">
    <button type="button" class="btn btn--secondary">새로고침</button>
  </div>
</div>

<div class="stack">          <!-- 세로 20px 간격 -->
  <section class="card">
    <header class="card__head">
      <div>
        <h2 class="card__title">섹션 제목</h2>
        <p class="card__desc">보조 설명</p>
      </div>
    </header>
    <div class="card__body"> ... </div>
    <div class="card__foot"> ... </div>   <!-- 선택 -->
  </section>
</div>
```

### 자주 쓰는 클래스

| 용도 | 클래스 |
|---|---|
| 버튼 | `btn` / `btn--secondary` / `btn--danger` / `btn--ghost` · 크기 `btn--sm` `btn--lg` `btn--block` |
| 입력 | `field` > `field__label` + `input` \| `select` \| `textarea`, 힌트 `field__hint` |
| 체크박스 | `<label class="check"><input type="checkbox"> 라벨</label>` |
| 폼 배치 | `form-grid` (자동 다열) · 검색줄 `filter-bar` + `filter-bar__actions` · 입력 1~2개면 `filter-bar--start` |
| 표 | `table` (`card__body--flush` 안에 넣는다) · 숫자 열 `num` · 동작 열 `actions-cell` |
| 상세 | `dl.kv` > `dt` / `dd` |
| 지표 타일 | `tiles` > `tile` > `tile__label` / `tile__value` / `tile__unit` / `tile__sub` |
| 원료코드 | `code-tag` (모노스페이스 + 왼쪽 악센트 바) |
| 인증·사유 | `chips` > `chip` |
| 점수 막대 | `meter` > `meter__track` > `meter__fill` (+ `meter__value`) |
| 알림 | `alert alert--error` / `--success` / `--info` |
| 빈 상태 | `empty` > `empty__icon` / `empty__title` / `empty__desc` |
| 로딩 | `skeleton-rows` > `skeleton` (목록) · `spinner` (버튼 안) |

### 상태 색은 CSS가 정한다 — 매핑 코드를 쓰지 말 것

배지와 행 색상은 **속성 선택자**로 결정된다. API 응답값을 그대로 꽂으면 된다.

```html
<span class="badge" :data-status="o.status">{{ o.status }}</span>
<span class="badge" :data-severity="a.severity">{{ a.severity }}</span>

<!-- 행 왼쪽 색 레일도 같은 방식 -->
<tr v-for="o in items" :data-status="o.status"> ... </tr>
```

인식하는 값: `CONFIRMED` `ACTIVE` `PAID` `정상` `LOW`(초록) /
`PENDING` `PROCESSING` `부족` `MEDIUM`(주황) /
`CANCELLED` `FAILED` `INACTIVE` `HIGH` `CRITICAL`(빨강) / `BUYER` `SUPPLIER`(파랑).
새 상태값이 생기면 JS가 아니라 `global.css`의 `.badge[data-status="..."]` 목록에 추가한다.

### 표는 모바일에서 카드로 접힌다 — `data-label`을 반드시 붙인다

860px 미만에서 표가 카드로 바뀐다. 이때 각 셀 왼쪽에 붙는 이름은
`data-label` 속성에서 가져온다. **빠뜨리면 모바일에서 라벨 없는 값만 보인다.**

```html
<td data-label="원료코드"><span class="code-tag">{{ m.materialCode }}</span></td>
<td data-label="단가" class="num">{{ formatNumber(m.unitPrice) }} 원</td>
<td class="actions-cell">...</td>   <!-- 동작 열은 data-label 없이 둔다 -->
```

### 해도 되는 것 / 하면 안 되는 것

- ✅ `<template>` 안의 태그·클래스는 자유롭게 바꾼다
- ✅ 새 스타일이 필요하면 `global.css`의 `@layer components`에 추가한다
- ❌ `.vue`에 `<style scoped>`를 넣지 않는다 (디자인이 파일마다 흩어진다)
- ❌ 인라인 `style=""`로 색을 지정하지 않는다 (토큰을 바꿔도 안 따라온다)

## 7. 반드시 지켜야 할 제약 — users 테이블의 `role` 컬럼

`auth-server`는 **소스가 없는 사전 빌드 이미지**이고, user-service와 **같은 `users` 테이블**을
`@Enumerated(EnumType.STRING)` + `enum Role { STUDENT, INSTRUCTOR }` 로 매핑한다.

따라서 `role` 컬럼에 `BUYER`/`SUPPLIER`를 저장하면, auth-server가 로그인 시 사용자를 로드하다가
`No enum constant ... BUYER` 예외로 **로그인이 깨진다.**

그래서 역할을 두 컬럼으로 분리했다.

| 컬럼 | 값 | 용도 |
|---|---|---|
| `role` | `STUDENT` / `INSTRUCTOR` | 인증 서버 호환 전용. **값을 늘리지 말 것** |
| `user_role` | `BUYER` / `SUPPLIER` | 도메인 역할. 프론트엔드가 쓰는 값 |

- 회원가입 시 user-service가 `BUYER → STUDENT`, `SUPPLIER → INSTRUCTOR`로 변환해 `role`에 함께 저장한다.
- `GET /api/users/me` 응답의 `role` 필드에는 **`user_role` 값(BUYER/SUPPLIER)** 이 실린다. 프론트 계약은 그대로다.
- `user_role`이 비어 있는 기존 계정(auth-server가 만든 시드 계정)은 `role`에서 역으로 유추한다.
  `INSTRUCTOR → SUPPLIER`, 그 외 → `BUYER`

### 로그인 테스트용 시드 계정

auth-server의 `DataInitializer`가 최초 기동 시 자동 생성한다.

| 이메일 | 비밀번호 | 프론트에서 보이는 역할 |
|---|---|---|
| `student@lecture.com` | `password1234` | BUYER (제약사·연구실) |
| `instructor@lecture.com` | `password1234` | SUPPLIER (공급 공장) |

## 8. 백엔드에 요청할 사항 (Sprint 1)

- [x] `User.UserRole` = `BUYER` / `SUPPLIER`, `companyName`·`businessNumber`·`gmpCertified` 필드 **(완료)**
- [x] `Material` 엔티티: `materialCode`, `casNumber`, `unit`, `availableCapacity`, `leadTimeDays`, `certifications`, `country` **(완료 · material-service)**
- [x] `GET /api/materials/code/{materialCode}/suppliers` (Ep-01 US1) **(완료)**
- [x] `PATCH /api/materials/{id}/capacity` (Ep-02 US2) **(완료)**
- [x] `GET /api/materials/my`, `GET /api/orders/my` — `X-User-Id` 기준 **(완료)**
- [x] `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/{id}/status`, `GET /api/orders/supplier` **(완료 · order-service)**
- [x] Gateway 라우팅: `/api/materials/**`, `/api/orders/**` **(완료)**
- [x] `GET /api/inventories/my`, `POST /api/inventories`, `PATCH /api/inventories/{id}/stock` **(완료 · order-service)**
- [x] `GET /api/inventories/alerts` — 부족 감지 스케줄러 결과 (Ep-02 US1) **(완료)**
- [x] `GET /api/recommend` — 예측 수요 + 소진 시뮬레이션 + 추천 공장 **(완료 · recommend-service)**
- [x] `POST /api/payments` · `GET /api/payments/my` **(완료 · payment-service)**
- [x] 결제 완료 → 주문 CONFIRMED **(완료 · Kafka `order.payment.completed`)**
- [x] Gateway 라우팅: `/api/inventories/**` **(완료)**
- [x] 목록 API 응답을 페이징 래퍼(`content` / `totalElements` / `totalPages`)로 통일 **(materials·orders 완료)**
- [x] 에러 응답을 `{ success:false, message, data:null }` 포맷으로 통일 **(materials·orders 완료)**

### 이미 만들어 둔 서비스에서 참고할 점

`material-service` / `order-service`가 규약의 기준입니다. 남은 서비스도 같은 방식으로 맞춰 주세요.

- **역할 헤더는 그대로 쓰면 안 됩니다.** 인증 서버가 `users.role`(STUDENT / INSTRUCTOR)을 JWT에 싣기
  때문에 `X-User-Role`로는 `BUYER` / `SUPPLIER`가 오지 않습니다.
  `support/CallerRole.java`처럼 두 표기를 모두 받아 정규화하세요.
- **내부 API는 경로만으로 못 막습니다.** 게이트웨이가 `/api/{도메인}/**`를 통째로 넘기고
  discovery locator도 켜져 있어, 로그인한 사용자면 누구나 `/internal/**`을 호출할 수 있습니다.
  `config/InternalApiInterceptor.java`처럼 `X-Internal-Key`를 요구하세요.
- **금액·단가는 클라이언트에서 받지 않습니다.** `order-service`는 `materialId`만 받고
  단가·공급사는 `material-service`에서 조회해 주문 시점 값으로 저장합니다.
