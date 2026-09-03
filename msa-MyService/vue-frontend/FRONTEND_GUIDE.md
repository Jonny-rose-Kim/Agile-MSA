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

## 6. 반드시 지켜야 할 제약 — users 테이블의 `role` 컬럼

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

## 7. 백엔드에 요청할 사항 (Sprint 1)

- [x] `User.UserRole` = `BUYER` / `SUPPLIER`, `companyName`·`businessNumber`·`gmpCertified` 필드 **(완료)**
- [ ] `Material` 엔티티: `materialCode`, `casNumber`, `unit`, `availableCapacity`, `leadTimeDays`, `certifications`, `country`
- [ ] 목록 API 응답을 페이징 래퍼(`content` / `totalElements` / `totalPages`)로 통일
- [ ] `GET /api/materials/code/{materialCode}/suppliers` 신규 (Ep-01 US1)
- [ ] `PATCH /api/materials/{id}/capacity` 신규 (Ep-02 US2)
- [ ] `GET /api/materials/my`, `GET /api/inventories/my`, `GET /api/orders/my` — 모두 `X-User-Id` 기준
- [ ] `GET /api/inventories/alerts` — 부족 감지 스케줄러 결과 (Ep-02 US1)
- [ ] `GET /api/recommend` — 예측 수요 + 추천 공장을 한 응답으로
- [ ] 에러 응답을 `{ success:false, message, data:null }` 포맷으로 통일 (message는 한글)
- [ ] Gateway 라우팅: `/api/materials/**`, `/api/inventories/**`, `/api/orders/**`, `/api/payments/**`, `/api/recommend/**`
