# Lovable 디자인 프롬프트 모음

> 목적: Lovable로 **화면 디자인만** 생성한 뒤, 마크업을 현재 Vue 프로젝트로 이식한다.
> 원칙: **API를 호출하는 버튼·입력 필드는 하나도 추가하거나 삭제하지 않는다.** 겉모습만 바꾼다.

---

## 0. 시작 전에 반드시 알아야 할 것

**Lovable은 Vue를 만들지 못한다.** React + Vite + Tailwind + shadcn/ui만 생성한다.
따라서 Lovable 결과물을 그대로 붙여넣을 수는 없고, **Tailwind 유틸리티 클래스를 공용어로 삼아 마크업만 옮긴다.**

```
Lovable (React JSX + Tailwind)  ──이식──>  현재 프로젝트 (Vue SFC + Tailwind)
   className="rounded-xl border p-6"        class="rounded-xl border p-6"   ← 문자열 동일
   {items.map(i => <tr>…</tr>)}             <tr v-for="i in items">…</tr>   ← 기계적 변환
```

그래서 프롬프트에 **"shadcn/ui·Radix·framer-motion 금지, Tailwind 유틸리티만"** 을 반드시 넣는다.
shadcn 컴포넌트를 쓰면 Vue에 없는 React 전용 코드가 섞여서 이식 비용이 몇 배로 뛴다.

### 작업 순서

| 단계 | 하는 일 | 도구 |
|---|---|---|
| 1 | Lovable에 아래 프롬프트를 순서대로 입력해 화면 생성 | Lovable |
| 2 | Vue 프로젝트에 Tailwind 설치 (§4 참고) | 로컬 |
| 3 | Lovable JSX → Vue `<template>` 로 클래스 문자열 이식 | 로컬 |
| 4 | `<script setup>` 은 **손대지 않는다** (API 배선 그대로) | 로컬 |

---

## 1. Lovable "Knowledge" 에 먼저 넣을 고정 컨텍스트

Lovable 프로젝트 설정 → **Knowledge**(또는 첫 메시지)에 아래를 그대로 붙여넣는다.
매 요청마다 다시 설명할 필요가 없어진다.

```
# Project Context

I am designing the UI for "원료의약품 수급 매칭 플랫폼" (Pharmaceutical Raw Material
Supply-Matching Platform), a Korean B2B web application.

## What it does
Pharmaceutical companies and research labs (BUYER) register their raw-material stock
levels and shortage thresholds. When stock falls below a threshold, the platform alerts
them and helps them find GMP-certified manufacturing plants (SUPPLIER) that have spare
production capacity for that material. Buyers then place procurement orders and pay.

## Two user roles
- BUYER  = 제약사 · 연구실 (buys materials)
- SUPPLIER = 공급 공장 (sells materials, has GMP/DMF certifications)
The navigation and several buttons differ by role.

## HARD CONSTRAINTS — these are not negotiable
1. This is a REDESIGN of an existing, already-working application. The set of screens,
   buttons, form fields, and table columns is FIXED. Do NOT add any new button, field,
   screen, or feature. Do NOT remove any either. Change only the visual design.
2. Do NOT write any data fetching, API calls, or backend integration. Do NOT connect
   Supabase or any database. Use hardcoded mock data defined at the top of each file.
   The real backend wiring already exists elsewhere and will be reattached by me.
3. Styling: Tailwind CSS utility classes written directly in JSX ONLY.
   Do NOT use shadcn/ui, Radix UI, Headless UI, framer-motion, or any component library.
   Icons: lucide-react only. Charts: none (use tables).
   Reason: I will port this markup to Vue 3, and Tailwind class strings transfer
   verbatim while React component libraries do not.
4. All visible UI text must stay in Korean, exactly as I write it in each prompt.
   Do not translate, rephrase, or "improve" the Korean labels.
5. Every screen must be fully responsive. On screens narrower than 768px, wide data
   tables must become stacked cards (label + value rows), never horizontal scroll.

## Art direction
Domain is pharmaceutical / regulated manufacturing: the design must read as precise,
clinical, and trustworthy — not playful, no purple SaaS gradients, no big hero blobs.

- Base palette: neutral slate/zinc. Background #F8FAFC, surfaces white, borders slate-200.
- Accent: a single deep clinical blue (#1D4ED8 family). Use it sparingly — primary
  buttons, active nav, focus rings.
- Semantic colors: 정상/CONFIRMED = emerald, 부족/PENDING = amber, 위험/CANCELLED/FAILED = red.
- Typography: Pretendard (fallback: Inter, system-ui) for Korean. Use tabular-nums for
  all numeric table cells so digits align.
- Density: compact and data-dense. This is an operational B2B tool, not a marketing site.
  Table rows ~44px tall, 8px spacing grid.
- Depth: rely on 1px borders and rounded-lg/xl corners, not heavy drop shadows.
- Motion: subtle only (hover/focus transitions ~150ms). No scroll animations.
- Light mode only.
```

---

## 2. 화면별 프롬프트

아래 프롬프트를 **순서대로 하나씩** 입력한다. 한 번에 전부 넣으면 Lovable이 화면을 뭉갠다.

### P1 — 디자인 시스템 + 앱 셸

```
Build the app shell and design system first. Create these routes as empty placeholder
pages for now: /, /login, /callback, /materials, /materials/new, /materials/:id,
/materials/:id/edit, /supplier/materials, /inventories, /inventories/alerts, /orders,
/orders/:id, /supplier/orders, /payments, /recommend, /mypage

## Global header (fixed at top, present on every page)
Left: brand link, text "원료의약품 수급 매칭"
Center: navigation links. These are role-dependent — build a mock role toggle in the
header (dev-only, top-right corner, small) so I can preview both states:
  - Always visible when logged in: "원료 카탈로그", "AI 추천"
  - BUYER only: "재고 관리", "부족 알림", "조달 주문", "결제 내역"
  - SUPPLIER only: "내 공급 품목", "수주 관리"
Right: when logged in, a link showing "김담당 (제약사 · 연구실)" that goes to /mypage,
       plus a "로그아웃" button. When logged out, a "로그인" link.
On mobile (<768px) the nav collapses into a hamburger drawer.

## Shared building blocks (define once, reuse everywhere)
Create these as plain React components using only Tailwind utilities:
- PageTitle (h1) + PageDesc (muted subtitle paragraph)
- Section (white card: rounded-xl, border, padding) with optional SectionTitle
- Button variants: primary (filled accent), secondary (outlined), danger (outlined red),
  each with a disabled state and a loading state that shows the label text swapped
  (e.g. "저장" -> "저장 중...")
- Form field: stacked label above input. Support input / select / textarea / checkbox.
- DataTable: header row + body rows, zebra-free, 1px borders, hover highlight.
  Below 768px it must render each row as a stacked card instead.
- Badge: small pill, with color variants for 정상/부족/위험 and for order statuses
  PENDING / CONFIRMED / CANCELLED / FAILED
- State blocks: Loading ("불러오는 중..."), Empty (dashed border, centered muted text),
  ErrorMessage (red tinted box), SuccessMessage (green tinted box)

## Landing page (/)
- Title: "원료의약품 수급 매칭 플랫폼"
- Subtitle: "재고 부족을 미리 감지하고, 공급 가능한 인증 공장을 즉시 찾습니다."
- Two explanatory lines:
  "제약사·연구실은 보유 원료의 재고와 임계치를 등록해 부족 시점을 미리 파악하고,"
  "공급 공장은 여유 생산능력을 등록해 실제 공급 가능한 물량만 노출합니다."
- Exactly ONE call-to-action button: "로그인 / 회원가입" when logged out,
  "서비스 시작하기" when logged in. Do not add any other button or section.
```

### P2 — 로그인 · 회원가입 · 콜백

```
Design /login, /callback.

## /login — a single page with two modes toggled in place (not two routes)

Mode A "로그인" (default). Page title: "로그인"
- Description: "계정으로 로그인합니다. 인증 서버로 이동합니다."
- Button (primary): "로그인"
- Button (secondary): "회원가입"  → switches to Mode B
Nothing else. There is NO email/password field in this mode — login redirects to an
external authorization server.

Mode B "회원가입". Page title: "회원가입"
A form with exactly these fields, in this order:
1. select, label "계정 유형 *", options: "제약사 · 연구실 (구매)" / "공급 공장 (판매)"
2. text input, label "공장명 *" when 공급 공장 is selected, otherwise "기관명 *".
   placeholder "한국API공장" / "그린제약" respectively
3. text input, label "사업자등록번호", placeholder "123-45-67890"
4. checkbox, label "GMP 인증 보유" — VISIBLE ONLY when 계정 유형 is 공급 공장
5. text input, label "담당자 이름 *", placeholder "홍길동"
6. email input, label "이메일 *", placeholder "user@example.com"
7. password input, label "비밀번호 * (8자 이상)"
- Submit button: "회원가입" (loading label: "가입 중...")
- Secondary button: "로그인으로"  → back to Mode A
- Slots above the form for an error message and a success message.
Do not add social login, "remember me", password confirmation, or terms checkboxes.

## /callback
A minimal centered status page. Default state: "로그인 처리 중입니다..." with a subtle
spinner. Error state: an error message box plus one button "로그인으로 돌아가기".

Make /login feel like the most polished screen in the app — it is the first impression.
Consider a two-column layout on desktop (brand/value proposition on the left, the form
card on the right) collapsing to a single column on mobile. But do not invent new
marketing copy beyond the lines already given on the landing page.
```

### P3 — 원료 카탈로그 (4개 화면)

```
Design the material screens.

## /materials — 원료 카탈로그
Title "원료 카탈로그", desc "공급 중인 원료를 검색합니다."
Filter bar (inline row on desktop, stacked on mobile) with exactly:
- text input "검색어", placeholder "원료명 · 원료코드 · CAS번호"
- select "카테고리": 전체 / 원료의약품(주성분) / 부형제 / 용매 / 시약 / 중간체 / 기타
- number input "최소 여유 생산능력", placeholder "500"
- select "인증": 전체 / GMP / DMF / ISO9001 / KGMP
- primary button "검색", secondary button "초기화"
Results table, columns exactly:
원료코드 | 원료명 | 카테고리 | 단가 | 여유 생산능력 | 리드타임 | 공급사 | (상세 링크)
Below the table, pagination: secondary "이전" / "1 / 5" / secondary "다음".
Include loading, empty ("조건에 맞는 원료가 없습니다."), and error states.

## /materials/:id — 원료 상세
Section 1 "원료 정보": a key-value detail list with rows
원료코드 / CAS 번호 / 카테고리 / 단가 / 최소 주문 수량 / 여유 생산능력 / 리드타임 / 인증 / 설명
Actions: secondary "수정" (SUPPLIER only), secondary "목록"
Section 2 "공급 가능 인증 공장 조회":
- number input "필요 수량 (KG)", placeholder "800"
- select "인증 조건": 전체 / GMP / DMF
- primary button "공급처 찾기"
- result table columns: 공장명 | 국가 | 여유 생산능력 | 리드타임 | 단가 | 인증 | (action)
  the action cell holds a primary button "조달 신청" (BUYER only)
- empty state text: "현재 조건을 만족하는 인증 공장이 없습니다. 필요 수량을 낮춰 다시 조회해 보세요."

## /materials/new and /materials/:id/edit — same form component
Title "공급 품목 등록" or "공급 품목 수정". Fields in order:
원료코드 * (disabled in edit mode) / 원료명 * / CAS 번호 / 카테고리 * (select) /
단위 * (select KG/L/EA) / 단가(원) * / 최소 주문 수량 / 여유 생산능력 * / 리드타임(일) * /
보유 인증 (4 checkboxes: GMP, DMF, ISO9001, KGMP) / 생산 국가 / 설명 (textarea)
Buttons: primary "등록" or "수정" (loading: "저장 중..."), secondary "취소".
Use a two-column field grid on desktop, single column on mobile.

## /supplier/materials — 내 공급 품목
Title "내 공급 품목", desc "여유 생산능력은 표에서 바로 수정할 수 있습니다."
Top actions: primary "공급 품목 등록", secondary "새로고침"
Table columns: 원료코드 | 원료명 | 단가 | 여유 생산능력 | 리드타임 | 상태 | (actions)
- The 여유 생산능력 cell contains an inline number input plus a small secondary button
  "저장" (loading label "저장 중"). Design this inline-edit cell carefully — it is the
  signature interaction of this screen.
- 상태 cell shows a badge.
- actions cell: a "수정" link and a danger button "공급 중단".
Empty state: "등록한 공급 품목이 없습니다."
```

### P4 — 재고 관리 · 부족 알림

```
Design the inventory screens (BUYER only).

## /inventories — 재고 관리
Title "재고 관리", desc "보유 원료의 재고량과 임계치를 등록합니다."
Section "재고 등록" — an inline form with exactly:
  text input "원료코드 *" (placeholder "API-CEFA-500"),
  number input "현재 재고량 *", number input "부족 임계치 *",
  primary button "등록" (loading: "등록 중...")
Table columns: 원료코드 | 원료명 | 현재고 | 임계치 | 상태 | (action)
- 현재고 cell: inline number input + small secondary button "저장"
- 상태 cell: badge reading "부족" (amber/red) or "정상" (emerald)
- action cell: a "공급처 찾기" link, shown only on 부족 rows
Empty: "등록된 재고가 없습니다."

## /inventories/alerts — 재고 부족 알림
Title "재고 부족 알림", desc "임계치 미만으로 감지된 원료입니다."
One secondary button "새로고침" above the table.
Table columns: 심각도 | 원료코드 | 원료명 | 현재고 | 임계치 | 부족량 | 감지 시각 | (action)
- 심각도 is a badge (severity levels: HIGH / MEDIUM / LOW)
- action cell: "공급처 찾기" link
Empty: "부족 알림이 없습니다."
Make severity visually scannable — someone should spot the critical rows in one glance
without reading. Use a left color rail on the row rather than only coloring the badge.
```

### P5 — 조달 주문 · 수주 관리 · 결제

```
Design the order and payment screens.

## /orders — 조달 주문 (BUYER)
Title "조달 주문", desc "조달을 신청하면 주문이 PENDING 상태로 생성됩니다."
Section "조달 신청" — inline form, exactly:
  number input "원료 ID *", number input "수량 *", date input "납기 희망일",
  primary button "조달 신청" (loading: "신청 중...")
Table columns: 주문번호 | 원료명 | 수량 | 금액 | 상태 | 신청일 | (상세 링크)
상태 is a status badge. Empty: "조달 주문 내역이 없습니다."

## /orders/:id — 주문 상세
Title "주문 상세". Key-value detail list:
주문번호 / 원료 / 수량 / 금액 / 상태 / 납기 희망일 / 신청일
The 상태 row shows a badge, and while the order is still pending it also shows the text
"— 결제 완료 이벤트를 기다리는 중..." with a subtle pulsing indicator.
Actions: secondary "새로고침", secondary "목록".

## /supplier/orders — 수주 관리 (SUPPLIER)
Title "수주 관리", desc "우리 공장이 받은 조달 주문입니다."
Filter row: select "상태" with options
  전체 / "PENDING (결제 대기)" / "CONFIRMED (결제 완료)" / "CANCELLED",
  plus primary button "조회"
Table columns: 주문번호 | 구매처 | 원료명 | 수량 | 상태 | 신청일 | (상세 링크)
Empty: "수주 내역이 없습니다."

## /payments — 결제 내역 (BUYER)
Title "결제 내역", desc "조달 주문에 대한 결제 내역입니다."
Section "결제 요청" — inline form: number input "주문번호 *",
  primary button "결제하기" (loading: "결제 중...")
Table columns: 결제번호 | 주문번호 | 금액 | 상태 | 거래ID | 결제일
Empty: "결제 내역이 없습니다."
```

### P6 — AI 추천 · 마이페이지

```
Design the remaining two screens.

## /recommend — AI 수요 예측 · 공장 추천
Title "AI 수요 예측 · 공장 추천"
desc "원료 하나를 입력하면 예측 수요와 추천 공장을 함께 받습니다."
Query form (inline): text input "원료코드 *" (placeholder "API-CEFA-500"),
  number input "필요 수량" (placeholder "800"), number input "예측 기간(일)" (placeholder "90"),
  primary button "조회" (loading state text: "예측 중...")
Result section 1 "수요 예측": key-value rows 예측 수요 / 신뢰도 / 예측 모델 / 반영 외부 지표,
  followed by a table with columns 일자 | 예측 | 하한 | 상한.
  Present the confidence value as a percentage with a small progress bar.
Result section 2 "공급 리스크 기반 추천 공장": table columns
  공장명 | 리스크 점수 | 등급 | 추천 사유 | (action)
  등급 is a badge; 리스크 점수 should read as a score, not a plain number.
  action cell: primary button "조달 신청" (BUYER only)
  Empty: "추천할 공장이 없습니다."
This is the flagship AI screen — make it the visual highlight, but stay within the
elements listed. No charting library; the forecast table stays a table.

## /mypage — 마이페이지
Title "마이페이지"
Section "내 정보": key-value rows 사용자 ID / 이름 / 이메일 / 계정 유형 (badge + label) /
  소속 / GMP 인증 (SUPPLIER only) / 가입일
  Actions: secondary "새로고침", danger "로그아웃"
Section "공급사 정보 조회":
  desc "원료 상세 화면에서 공급사 정보를 표시할 때 쓰는 API입니다."
  number input "사용자 ID" (placeholder "7") + primary button "조회"
  result key-value rows: 이름 / 계정 유형 / 소속 / GMP 인증
```

### P7 — 반응형 · 마감

```
Final pass. Do not add or remove any element — polish only.

1. Verify every screen at 375px, 768px, 1280px, and 1920px width.
   No horizontal page scroll at any width.
2. Every data table must collapse into stacked cards below 768px, with the column name
   as the label. Inline-edit cells (여유 생산능력, 현재고) must stay usable in card form.
3. Keyboard and a11y: visible focus rings on every interactive element, labels bound to
   inputs via htmlFor/id, aria-live on the error and success message slots.
4. Numeric table cells: right-aligned, tabular-nums, thousands separators (ko-KR).
5. Long values (원료명, 추천 사유) must truncate with ellipsis and a title tooltip
   rather than wrapping the table.
6. Give every button an explicit disabled style, and make sure loading labels
   ("저장 중...", "가입 중...", "신청 중...", "결제 중...", "예측 중...") are wired to
   a mock loading toggle so I can see them.
7. Print the final Tailwind theme (colors, font sizes, radii, spacing) into
   src/index.css using @theme, so I can copy the token block out in one piece.
```

---

## 3. 프롬프트 사용 요령

- **한 번에 한 프롬프트.** P1이 끝나고 결과를 확인한 뒤 P2로 넘어간다.
- 마음에 안 드는 부분은 **화면 단위로** 다시 요청한다. 예:
  `"Only redesign /materials. Keep every filter, button, and column exactly as is. Make the filter bar feel lighter — try a single rounded search bar with the three secondary filters as compact dropdowns beside it."`
- Lovable이 버튼을 새로 만들거나 지우면 즉시:
  `"You added/removed an element. The element set is fixed. Restore it exactly and change only the styling."`
- Supabase 연결을 제안하면 전부 거절한다. 이 프로젝트는 백엔드가 이미 있다.

---

## 4. Vue 프로젝트로 이식하기

### 4-1. Tailwind 설치

```bash
cd msa-MyService/vue-frontend
npm i -D tailwindcss @tailwindcss/vite
npm i lucide-vue-next          # Lovable이 lucide-react를 썼을 때만
```

`vite.config.js` 에 플러그인 추가:

```js
import tailwindcss from '@tailwindcss/vite'
export default defineConfig({ plugins: [vue(), tailwindcss()] })
```

`src/assets/styles/global.css` 맨 위에 추가하고, Lovable이 P7에서 뽑아준 `@theme` 토큰
블록을 그 아래에 붙여넣는다:

```css
@import "tailwindcss";

@theme {
  /* Lovable이 출력한 토큰을 여기에 */
}
```

기존 임시 스타일(`.section`, `.badge`, `.todo-box` 등)은 이식이 끝난 화면부터
하나씩 지운다. **`.todo-box` 는 팀원 안내용이므로 디자인 단계에서 전부 삭제한다.**

### 4-2. JSX → Vue 템플릿 변환 규칙

| React (Lovable) | Vue (이 프로젝트) |
|---|---|
| `className="..."` | `class="..."` |
| `{items.map(i => <tr key={i.id}>…</tr>)}` | `<tr v-for="i in items" :key="i.id">…</tr>` |
| `{cond && <div>…</div>}` | `<div v-if="cond">…</div>` |
| `{a ? <X/> : <Y/>}` | `<X v-if="a" /><Y v-else />` |
| `onClick={fn}` | `@click="fn"` |
| `value={v} onChange={…}` | `v-model="v"` |
| `htmlFor` | `for` |
| `disabled={loading}` | `:disabled="loading"` |
| `{value}` | `{{ value }}` |

### 4-3. 절대 건드리지 말 것

- 모든 `.vue` 파일의 `<script setup>` 블록 — API 배선이 여기 들어 있다
- `src/api/*.js`, `src/store/auth.js`, `src/composables/useAsync.js`, `src/router/index.js`
- `v-model` 이 물려 있는 변수명, `@click` 에 연결된 함수명

`<template>` 안의 **클래스와 태그 구조만** 바꾼다. 그러면 API 호출은 그대로 동작한다.

### 4-4. 이식 후 확인

```bash
npm run dev
```

화면마다: 버튼 개수가 그대로인지, 폼 제출이 되는지, 로딩/에러/빈 상태가 뜨는지 확인한다.

---

## 5. 화면·요소 인벤토리 (변경 금지 목록)

이식 후 대조용. 이 표의 요소가 하나라도 빠지면 API 호출이 끊긴 것이다.

| 화면 | 호출 API | 필수 인터랙션 요소 |
|---|---|---|
| AppHeader | — | 로고 링크, 역할별 nav 링크 7종, 사용자명(→/mypage), 로그아웃 |
| `/` | — | CTA 버튼 1개 |
| `/login` | `POST /api/users/register`, `GET /oauth2/authorize` | 로그인, 회원가입(모드전환), 가입폼 7필드, 회원가입 제출, 로그인으로 |
| `/callback` | `POST /oauth2/token`, `GET /api/users/me` | 로그인으로 돌아가기(에러 시) |
| `/materials` | `GET /api/materials` | 필터 4개, 검색, 초기화, 이전/다음, 행별 상세 링크 |
| `/materials/:id` | `GET /api/materials/{id}`, `GET /api/materials/code/{code}/suppliers` | 수정, 목록, 필요수량, 인증조건, 공급처 찾기, 행별 조달 신청 |
| `/materials/new`·`/edit` | `POST`·`PUT /api/materials` | 12필드, 등록/수정, 취소 |
| `/supplier/materials` | `GET /api/materials/my`, `PATCH .../capacity`, `DELETE .../{id}` | 공급 품목 등록, 새로고침, 행별 인라인 수량+저장, 수정 링크, 공급 중단 |
| `/inventories` | `POST`·`GET /api/inventories/my`, `PATCH .../stock` | 등록폼 3필드+등록, 행별 인라인 재고+저장, 공급처 찾기 링크 |
| `/inventories/alerts` | `GET /api/inventories/alerts` | 새로고침, 행별 공급처 찾기 링크 |
| `/orders` | `POST /api/orders`, `GET /api/orders/my` | 신청폼 3필드+조달 신청, 행별 상세 링크 |
| `/orders/:id` | `GET /api/orders/{id}`, `GET .../status` | 새로고침, 목록 |
| `/supplier/orders` | `GET /api/orders/supplier` | 상태 select, 조회, 행별 상세 링크 |
| `/payments` | `POST /api/payments`, `GET /api/payments/my` | 주문번호, 결제하기 |
| `/recommend` | `GET /api/recommend` | 원료코드, 필요수량, 예측기간, 조회, 행별 조달 신청 |
| `/mypage` | `GET /api/users/me`, `GET /api/users/{id}` | 새로고침, 로그아웃, 사용자 ID, 조회 |
