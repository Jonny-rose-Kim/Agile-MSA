# 원료의약품 수급 매칭 플랫폼

재고 부족을 조기에 감지하고 공급 가능한 인증 공장을 즉시 찾아주는 MSA 기반 서비스.

---

## ⚠️ 먼저 확인 — 저장소만 클론해서는 실행되지 않습니다

`auth-server`와 `api-gateway`는 **소스가 없는 사전 빌드 이미지**이고, 용량 문제로 저장소에서 제외했습니다.
(GitHub 100MB 제한 초과) Docker Hub에서도 받을 수 없으므로 **이미지 tar 파일을 별도로 받아야 합니다.**

| 필요한 파일 | 용량 | 받는 곳 |
|---|---|---|
| `msa-lecture-images-arm64.tar` (Apple Silicon) | 1.2GB | 팀 공유 드라이브 / 실습 자료 |
| `infra-images.tar` (Intel Mac · Windows) | 343MB | 팀 공유 드라이브 / 실습 자료 |

> 둘 중 **하나만** 받으면 됩니다. `msa-MyService/` 폴더 안에 두세요.
> mariadb, kafka는 Docker Hub에서 자동으로 받아집니다.

---

## 사전 준비물

| 항목 | 버전 | 비고 |
|---|---|---|
| Docker Desktop | 최신 | 실행 중이어야 함 |
| Node.js | 20 이상 | 프론트엔드 개발 서버용 |
| JDK | 불필요 | 도커 컨테이너 안에서 빌드됨 |

---

## 실행 순서

```bash
# 1) 클론
git clone https://github.com/Jonny-rose-Kim/Agile-MSA.git
cd Agile-MSA/msa-MyService

# 2) 이미지 tar를 이 폴더(msa-MyService/)에 복사한 뒤 로드
docker load -i msa-lecture-images-arm64.tar     # Apple Silicon
# docker load -i infra-images.tar               # Intel Mac / Windows

# 3) 백엔드 빌드 및 기동 (첫 실행은 10분 이상 걸릴 수 있음)
docker compose build
docker compose up -d

# 4) 기동 확인 — 모든 서비스가 Eureka에 등록되면 준비 완료
docker compose ps
open http://localhost:8761

# 5) 프론트엔드 (새 터미널)
cd vue-frontend
npm install
npm run dev
```

브라우저에서 **http://localhost:3000** 접속.

---

## 테스트 계정

`auth-server`가 최초 기동 시 자동 생성합니다. 회원가입 없이 바로 로그인할 수 있습니다.

| 이메일 | 비밀번호 | 역할 |
|---|---|---|
| `student@lecture.com` | `password1234` | BUYER (제약사 · 연구실) |
| `instructor@lecture.com` | `password1234` | SUPPLIER (공급 공장) |

회원가입으로 새 계정을 만들 수도 있습니다. (계정 유형 BUYER / SUPPLIER 선택)

---

## 서비스 구성

| 서비스 | 포트 | 담당 도메인 |
|---|---|---|
| api-gateway | 8080 | 라우팅 · JWT 검증 · `X-User-Id` 주입 |
| auth-server | 9000 | OAuth2 인가 · 토큰 발급 |
| eureka-server | 8761 | 서비스 레지스트리 |
| user-service | 8081 | 회원 · 권한 (BUYER / SUPPLIER) |
| material-service | 8082 | 원료 카탈로그 · 공급 가능 공장 |
| order-service | 8083 | 재고 · 임계치 · 조달 주문 |
| payment-service | 8084 | 조달 결제 |
| recommend-service | 8085 | AI 수요 예측 · 공장 추천 |
| vue-frontend | 3000 | 프론트엔드 |

> 프론트엔드는 **Gateway(8080)만** 호출합니다. 개별 서비스 포트를 직접 호출하지 않습니다.

---

## 담당 분배

| 담당 API | 수정할 파일 |
|---|---|
| `/api/users` · 로그인 전체 | `api/user.js`, `api/auth.js`, `store/auth.js`, `views/Login·Callback·MyPage` |
| `/api/materials` | `api/material.js`, `views/material/` |
| `/api/inventories` | `api/inventory.js`, `views/inventory/` |
| `/api/orders` | `api/order.js`, `views/order/` |
| `/api/payments` | `api/payment.js`, `views/payment/` |
| `/api/recommend` | `api/recommend.js`, `views/recommend/` |

**담당 폴더 밖의 파일은 건드리지 않습니다.** 공통 파일(`api/index.js`, `store/auth.js`,
`router/index.js`, `composables/useAsync.js`, `global.css`)은 프론트엔드 담당자에게 요청하세요.

---

## 문서

| 문서 | 내용 |
|---|---|
| [05-API명세_요약.md](05-API명세_요약.md) | 프론트엔드가 호출하는 엔드포인트 전체 목록 (한 표) |
| [05-API명세.md](05-API명세.md) | Request/Response 예시까지 포함한 상세 명세 |
| [06-프론트엔드-API배선흐름.md](06-프론트엔드-API배선흐름.md) | API 호출 배선 흐름 다이어그램 |
| [msa-MyService/vue-frontend/FRONTEND_GUIDE.md](msa-MyService/vue-frontend/FRONTEND_GUIDE.md) | 프론트엔드 코딩 규약 · role 컬럼 제약 |

---

## 트러블슈팅

### 로그인 버튼을 눌렀는데 "페이지를 로드하지 못함"

백엔드가 떠 있지 않은 경우입니다. 로그인은 `http://localhost:8080/oauth2/authorize`로
브라우저를 이동시키므로, Gateway가 없으면 브라우저 레벨 오류가 납니다.

```bash
docker compose ps                        # 컨테이너가 모두 Up 인지 확인
curl -I http://localhost:8080/login      # 200이면 정상
```

### 포트 충돌 — `address already in use`

다른 프로젝트가 같은 포트를 쓰고 있는 경우입니다.

```bash
lsof -i:8081                             # 점유 프로세스 확인
```

해당 앱을 종료하거나, `docker-compose.override.yml`을 만들어 호스트 포트만 바꿉니다.
(이 파일은 `.gitignore`에 있어 팀원에게 전파되지 않습니다)

```yaml
services:
  user-service:
    ports: !override
      - "8091:8081"
```

### 빌드 중 `429 Too Many Requests`

Maven Central(`repo.maven.apache.org`)의 요청 제한입니다.
**같은 네트워크를 쓰는 팀원들은 공인 IP가 하나로 묶여** 동시에 빌드하면 금방 한도에 걸립니다.

대응은 이미 적용되어 있습니다. 각 서비스의 `settings.gradle`·`build.gradle`에
Google 미러(`maven-central.storage-download.googleapis.com`)를 우선 저장소로 등록해 두었습니다.
그래도 429가 뜬다면 아래를 확인하세요.

```bash
git pull                      # 미러 설정이 반영된 최신 코드인지 확인
docker compose build          # --no-cache 를 붙이지 말 것
```

> `--no-cache`는 매번 의존성 전체를 다시 받아 429를 유발합니다.
> 빌드가 꼬였을 때만 쓰고, 평소에는 붙이지 마세요.

### `users` 테이블의 `role` 컬럼에 BUYER/SUPPLIER를 넣지 마세요

`auth-server`(사전 빌드 이미지)가 같은 `users` 테이블을 `enum Role { STUDENT, INSTRUCTOR }`로
매핑합니다. `role` 컬럼에 다른 값이 들어가면 **전체 로그인이 깨집니다.**
도메인 역할은 `user_role` 컬럼에 따로 저장하며, user-service가 자동으로 변환합니다.
자세한 내용은 [FRONTEND_GUIDE.md](msa-MyService/vue-frontend/FRONTEND_GUIDE.md) 6장 참고.
