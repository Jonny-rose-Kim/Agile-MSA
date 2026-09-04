# material-service — course-service 대비 변경 내역

강의용 `course-service`(강의 도메인)를 원료의약품 도메인 `material-service`로 개편했다.
아래는 **기존 course-service 대비 추가·수정한 부분만** 정리한 것이다.

---

## 1. 패키지 · 설정

| 항목 | course-service | material-service |
|---|---|---|
| 패키지 | `com.lecture.course` | `com.lecture.material` |
| 포트 | 8082 | **8086** |
| DB | `lecture_db` (공용) | **`material_db`** (서비스별 독립 스키마) |
| 테이블 | `courses` | `materials` + `material_certifications` |
| `settings.gradle` / `build.gradle` | mavenCentral만 | **Google 미러 저장소 추가** (Maven Central 429 대비) |
| `application.yml` | port 8082 / lecture_db | port 8086 / material_db (`?createDatabaseIfNotExist=true`) |

---

## 2. 엔티티 교체 — `Course` → `Material`

course의 필드(title, description, category, price, instructorId, enrollmentCount, status)를
아래 필드로 전면 교체.

| 필드 | 타입 | 비고 |
|---|---|---|
| `materialCode` | String | 공급사 내 중복 불가 |
| `materialName` | String | |
| `description` | String (TEXT) | |
| `casNumber` | String | 화학물질 식별번호 |
| `category` | enum | API_INGREDIENT / EXCIPIENT / SOLVENT / REAGENT / INTERMEDIATE / OTHER |
| `unit` | enum | KG / L / EA |
| `unitPrice` | BigDecimal | |
| `minOrderQuantity` | Integer | |
| `supplierId` | Long | 공급사 user ID (X-User-Id로 설정) |
| `supplierName` | String | user-service에서 조회해 비정규화 저장 |
| `country` | String | 생산 국가 |
| `availableCapacity` | Integer | **여유 생산능력 (Ep-02 US2)** |
| `leadTimeDays` | Integer | 리드타임 |
| `certifications` | List\<String\> | GMP / DMF / ISO9001 / KGMP — `@ElementCollection` |
| `status` | enum | ACTIVE / INACTIVE |
| `createdAt` / `updatedAt` | LocalDateTime | JPA Auditing (course와 동일) |

---

## 3. 새로 추가한 파일 (course-service에 없던 것)

| 파일 | 역할 |
|---|---|
| `repository/MaterialSpecs.java` | JPA **Specification 동적 검색** — keyword·category·certification·minCapacity 조건 조합 (course는 파생 쿼리만) |
| `client/UserClient.java` | **WebClient로 user-service 호출** → 공급사명 조회 (course는 서비스 간 호출 없음) |
| `config/WebClientConfig.java` | `@LoadBalanced WebClient.Builder` 빈 |
| `exception/ApiException.java` | HTTP 상태코드 **404 / 403 / 409 구분** (course는 IllegalArgumentException=400만) |

## 4. 유지하되 내용 수정한 파일

| 파일 | course-service | material-service |
|---|---|---|
| `entity/*.java` | `Course` | `Material` (§2) |
| `dto/*.java` | `CourseDto` — CreateRequest, CourseResponse, ApiResponse | `MaterialDto` — CreateRequest, UpdateRequest, **CapacityRequest**, MaterialResponse, **PageResponse\<T\>**(페이징 래퍼), **SuppliersResponse / SupplierItem** |
| `repository/*.java` | `CourseRepository` (JpaRepository) | `MaterialRepository` (JpaRepository + **JpaSpecificationExecutor**) |
| `service/*.java` | `CourseService` — 등록, 목록, 카테고리별 조회 | `MaterialService` — 검색, 상세, 공급처 매칭, 등록/수정/생산능력갱신/soft delete, 권한·소유권 검증 |
| `controller/*.java` | `CourseController` `/api/courses` (엔드포인트 4 + internal 3) | `MaterialController` `/api/materials` (엔드포인트 8 + internal 3) |
| `config/GlobalExceptionHandler.java` | IllegalArgument/Validation/일반 | + **ApiException 핸들러**(상태코드 매핑) 추가 |
| `config/SecurityConfig.java` | permitAll | permitAll (동일) — 단 서비스 로직에서 X-User-Role 검증 추가 |

---

## 5. 기능 (엔드포인트) — course 대비 차이

| # | Method / URL | course에 대응 | 설명 |
|---|---|---|---|
| 1 | `GET /api/materials` | 전체목록·카테고리별만 있었음 | **통합검색**(원료명·코드·CAS) + 인증·생산능력 필터 + **페이징·정렬** |
| 2 | `GET /api/materials/{id}` | 있음 | 상세 |
| 3 | `GET /api/materials/code/{materialCode}/suppliers` | **없음 (신규)** | **대체 공급처 자동 조회 (Ep-01 US1)** — 동일 원료코드를 공급하는 여러 공장을 필요 수량·인증으로 필터링, 리드타임순 정렬, `suppliable` 플래그 |
| 4 | `POST /api/materials` | 있음 | 등록 — **SUPPLIER만(403)**, 공급사 내 코드 중복 시 **409** |
| 5 | `PUT /api/materials/{id}` | **없음 (신규)** | 수정 — **본인 소유만(403)** |
| 6 | `PATCH /api/materials/{id}/capacity` | **없음 (신규)** | **여유 생산능력만 부분 갱신 (Ep-02 US2)** — 인라인 편집용 |
| 7 | `GET /api/materials/my` | **없음 (신규)** | 내 공급 품목 (X-User-Id 기준) |
| 8 | `DELETE /api/materials/{id}` | **없음 (신규)** | 공급 중단 — **soft delete** (status=INACTIVE). course는 삭제 기능 자체가 없었음 |
| int | `GET /internal/{id}` | 유사 (`/internal/{id}`) | order-service가 주문 생성 시 원료·단가 조회 |
| int | `GET /internal/exists/{id}` | 동일 | 원료 존재 확인 |
| int | `POST /internal/{id}/capacity-deduct` | **없음 (신규)** | 결제 완료 후 여유 생산능력 차감 |

---

## 6. 권한 처리 (course에는 없던 로직)

- course-service: `SecurityConfig`가 `permitAll`이고 권한 검사 없음.
- material-service: 등록·수정·삭제·생산능력갱신 시
  1. Gateway가 주입한 **`X-User-Role`** 확인 (`INSTRUCTOR` = SUPPLIER, 아니면 403)
  2. 대상 원료의 **`supplierId` == 요청자 X-User-Id** 인지 소유권 검증 (아니면 403)

---

## 7. 서비스 밖 함께 추가/수정한 것

| 파일 | 내용 |
|---|---|
| `../gateway/routes.yml` | prebuilt api-gateway 이미지에 없는 `/api/materials/**` 라우트 오버레이 (`SPRING_CONFIG_ADDITIONAL_LOCATION`으로 주입) |
| `../init-db/00_databases.sql` | `material_db` 생성 및 `manager` 계정 권한 부여 |
| `../docker-compose.yml` | `course-service` 블록 → `material-service` 블록으로 교체 |
