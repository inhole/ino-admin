# 재사용 가능한 Admin Starter 프로젝트 개발 실행 계획

> Spring Boot 백엔드와 React/Vite/shadcn 기반 관리자 애플리케이션을 먼저 실제로 완성한 뒤, 검증된 기능만 재사용 가능한 멀티모듈로 단계적으로 추출한다.

## 1. 문서 목적

이 문서는 Codex와 함께 재사용 가능한 관리자 시스템을 설계·개발·검증·배포하기 위한 실행 기준이다. 단순 샘플이나 기능 모음이 아니라, 새 프로젝트가 관리자 기능을 빠르게 시작할 수 있는 **Admin Starter + reusable modules**를 만드는 것을 목표로 한다.

핵심 접근 방식은 다음과 같다.

1. `admin-server`와 `admin-web`에서 사용자 흐름을 먼저 끝까지 구현한다.
2. 통합 테스트와 실제 사용으로 기능 및 인터페이스를 검증한다.
3. 두 개 이상의 사용처가 있거나 경계가 안정된 기능만 `common-*` 모듈로 추출한다.
4. 애플리케이션 고유 정책, API DTO, 화면은 실행 애플리케이션에 남긴다.
5. 각 Phase는 독립적으로 실행·검증·커밋할 수 있는 크기로 나눈다.

---

## 2. 프로젝트 비전과 성공 기준

### 2.1 비전

- 인증, RBAC, 메뉴, 파일, 게시판, Excel, 감사 로그, 코드 생성 기능을 갖춘 실사용 가능한 관리자 시스템
- 새 Spring Boot 프로젝트가 필요한 모듈만 선택하여 사용할 수 있는 구조
- 로컬 실행부터 테스트, 문서화, 배포까지 자동화된 개발자 경험
- Codex가 작은 작업 단위로 안전하게 구현하고 스스로 검증할 수 있는 저장소

### 2.2 최종 성공 기준

- 신규 개발자가 문서만 보고 30분 이내에 로컬 환경을 실행할 수 있다.
- 관리자 로그인 후 사용자·역할·권한·메뉴를 관리할 수 있다.
- Local/S3 저장소를 설정만으로 교체할 수 있다.
- 게시판 CRUD, 첨부 파일, Excel 다운로드/업로드가 동작한다.
- 주요 보안 및 데이터 변경 행위가 Audit Log에 기록된다.
- 코드 생성기로 최소 CRUD 골격을 생성하고 빌드·테스트할 수 있다.
- 공통 모듈이 특정 관리자 화면이나 프로젝트 DTO에 의존하지 않는다.
- 백엔드·프론트엔드 테스트와 정적 검사가 CI에서 자동 실행된다.

### 2.3 비목표

MVP에서는 다음을 우선 구현하지 않는다.

- 마이크로서비스 전환
- 멀티테넌시
- 소셜 로그인 및 외부 IdP 통합
- 복잡한 BPM/워크플로 엔진
- 플러그인 마켓플레이스
- 모든 DBMS 및 클라우드 스토리지 지원
- 무제한 범용 코드 생성

---

## 3. 기술 스택 및 기본 결정

버전은 프로젝트 착수 시점의 안정 버전을 선택하고 Renovate 또는 Dependabot으로 관리한다. 버전 변경은 ADR에 근거를 남긴다.

### 3.1 Backend

- Java 25 LTS
- Spring Boot 4.1.x
- Gradle Kotlin DSL, Gradle multi-project
- Spring Web, Validation, Security
- Spring Data JPA + QueryDSL 또는 명시적 동적 조회 계층
- PostgreSQL
- Flyway
- JWT Access Token + Refresh Token
- springdoc-openapi
- Testcontainers, JUnit 5, Mockito, REST Assured 또는 MockMvc
- AWS SDK v2 기반 S3 adapter
- Apache POI 또는 검증된 Excel 라이브러리

### 3.2 Frontend

- React + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- React Router
- TanStack Query
- React Hook Form + Zod
- Vitest + React Testing Library
- Playwright
- OpenAPI 기반 API client 생성 또는 얇은 typed client

### 3.3 Platform

- Docker Compose: PostgreSQL, MinIO, 선택적으로 Redis/Mailpit
- GitHub Actions 기준 CI/CD
- 컨테이너 이미지 배포
- JSON 구조화 로그와 health/readiness endpoint

### 3.4 초기 아키텍처 결정

- 시작은 **모듈형 모놀리스**로 한다.
- REST API prefix는 `/api/v1`로 통일한다.
- 서버는 stateless access token을 사용하고 refresh token은 서버에서 폐기 가능하도록 저장한다.
- 권한은 `resource:action` 형태의 문자열 키를 사용한다. 예: `user:read`, `user:create`.
- 메뉴 접근과 API 권한은 연결할 수 있지만 동일 개념으로 취급하지 않는다.
- Entity를 API 응답으로 직접 노출하지 않는다.
- 공통 모듈은 가능한 한 Spring Boot 애플리케이션 진입점을 갖지 않는다.

---

## 4. 목표 저장소 및 디렉터리 구조

```text
admin-starter/
├─ AGENTS.md
├─ PROJECT_PLAN.md
├─ README.md
├─ CHANGELOG.md
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/
├─ docs/
│  ├─ architecture/
│  │  ├─ context.md
│  │  ├─ module-boundaries.md
│  │  └─ adr/
│  ├─ api/
│  ├─ database/
│  ├─ operations/
│  └─ prompts/
├─ apps/
│  ├─ admin-server/
│  │  └─ src/{main,test}/java/com/ino/admin/
│  │     ├─ identity/       # 사용자, 역할, 권한
│  │     ├─ menu/
│  │     ├─ file/
│  │     └─ ...             # 앱 전용 업무 vertical slice
│  └─ admin-web/
│     ├─ src/
│     ├─ tests/
│     └─ package.json
├─ modules/
│  ├─ common-core/
│  ├─ common-web/
│  ├─ common-security/
│  ├─ common-file/
│  ├─ common-file-s3/
│  ├─ common-audit/
│  ├─ common-excel/
│  └─ common-codegen/
├─ tools/
│  ├─ codegen-cli/
│  └─ scripts/
├─ infra/
│  ├─ docker/
│  ├─ compose.yaml
│  └─ monitoring/
└─ .github/
   ├─ workflows/
   └─ pull_request_template.md
```

### 4.1 모듈 책임

| 모듈 | 책임 | 포함하지 않는 것 |
|---|---|---|
| `common-core` | 공통 오류 코드, 시간/ID 추상화, 페이지 모델, 기반 타입 | HTTP, Security, JPA Entity, 업무 정책 |
| `common-web` | 공통 API 응답, 예외 변환, 요청 추적 ID, 웹 설정 | 업무 Controller, 화면별 DTO |
| `common-security` | 인증 principal, JWT 처리, 보안 확장점, 권한 검사 기반 | 사용자 화면, 조직별 권한 정책 |
| `common-file` | 저장소 port, Local adapter와 기본 auto-configuration | S3 SDK, 관리자 전용 API와 화면 |
| `common-file-s3` | 선택적 S3 adapter, properties와 client auto-configuration | Local-only consumer에 필수적인 기본 계약 |
| `common-audit` | 저장소 독립적 감사 actor/event 계약과 writer port | servlet 문맥, 로그인 계정 필드, 화면별 검색 정책 |
| `common-excel` | export/import 기반 계약, 변환·검증 지원 | 특정 게시판/사용자 컬럼 정의 |
| `common-codegen` | schema/template 처리, 생성 규칙, overwrite 보호 | 모든 업무 규칙 자동 추론 |
| `admin-server` 업무 패키지 | 도메인별 use case, entity, repository, 정책과 실행 조립 | 재사용 가능한 핵심 구현의 중복 |
| `admin-web` | 관리자 UX, routing, 화면 권한, API 사용 | 서버 권한 판정 |

### 4.2 의존성 규칙

```text
admin-server 업무 패키지 ─────> common-*
common-web ───────────────────> common-core
common-security ──────────────> common-core
common-file/common-audit ─────> common-core
common-file-s3 ─────────────> common-file
```

- `common-*`는 `apps/*`를 참조하지 않는다.
- admin-server의 업무 패키지 간 직접 참조는 최소화하고 명시적인 use case/event를 사용한다.
- 순환 의존은 허용하지 않는다.
- 모듈 경계는 ArchUnit 테스트로 검사한다.

---

## 5. 개발 원칙

### 5.1 Vertical Slice 우선

DB 테이블만 모두 만든 뒤 UI를 한꺼번에 만드는 방식 대신, 사용자 흐름 하나를 UI부터 DB까지 완성한다.

예: 로그인 화면 → 인증 API → JWT 발급 → 보호 API 호출 → 오류 처리 → 테스트.

### 5.2 앱 우선, 추출은 나중

기능은 먼저 `admin-server`의 업무 패키지 안에서 구현한다. 다음 조건이 충족되면 `common-*`로 추출한다.

- 실제 통합 흐름이 테스트되었다.
- API가 최소 한 차례 사용되며 불편한 점이 확인되었다.
- 프로젝트 고유 정책과 범용 기능을 구분할 수 있다.
- 추출 후에도 애플리케이션 동작이 동일함을 회귀 테스트로 증명할 수 있다.

### 5.3 기본 품질 규칙

- 모든 DB 변경은 Flyway migration으로 관리한다.
- 보안 경계는 프론트엔드가 아니라 서버에서 강제한다.
- 공개 API 변경에는 테스트와 문서 갱신이 필요하다.
- 시간은 서버에서 UTC로 저장하고 UI에서 locale/timezone으로 표시한다.
- 삭제는 데이터 성격에 따라 soft delete 또는 hard delete를 명시적으로 선택한다.
- 민감 정보, token, 비밀번호는 로그에 남기지 않는다.

---

## 6. 단계별 실행 계획

## Phase 0. 저장소 부트스트랩과 결정 고정

### 목표

개발자가 같은 방식으로 빌드·실행·검증할 수 있는 최소 골격을 만든다.

### 작업 목록

- [ ] Gradle multi-project와 Java toolchain 구성
- [ ] `admin-server`, `admin-web`, 초기 `common-core` 생성
- [ ] Vite + React + TypeScript + shadcn/ui 초기화
- [ ] formatter/linter/editor 설정
- [ ] Docker Compose로 PostgreSQL과 MinIO 구성
- [ ] `.env.example` 작성, 실제 secret 제외
- [ ] `/actuator/health`와 프론트 기본 화면 연결
- [ ] CI 기본 workflow: backend build/test, frontend lint/test/build
- [ ] README에 1-command 시작 절차 작성
- [ ] ADR-001 모듈형 모놀리스, ADR-002 JWT, ADR-003 PostgreSQL 작성

### 완료 기준(DoD)

- 새 clone에서 문서의 명령으로 서버·웹·DB가 실행된다.
- 서버 health와 웹 기본 화면을 확인할 수 있다.
- 백엔드와 프론트엔드 빌드 및 테스트가 CI에서 통과한다.
- 환경 변수 누락 시 이해 가능한 오류 메시지가 나온다.

---

## Phase 1. 공통 웹 기반과 DB 골격

### 목표

후속 기능이 공유할 API 규약, 오류 처리, migration, 관측성의 최소 기반을 만든다.

### 작업 목록

- [ ] API 응답/오류 형식 정의
- [ ] global exception handler, validation error mapping 구현
- [ ] correlation/request ID와 구조화 로그 적용
- [ ] JPA auditing과 `created_at`, `updated_at` 규칙 적용
- [ ] 공통 pagination/sort 요청 규약 구현
- [ ] Flyway baseline migration 작성
- [ ] OpenAPI 문서와 로컬 Swagger UI 제공
- [ ] 프론트 API client, 공통 오류 toast, loading/empty/error 상태 구현
- [ ] 접근성 기준과 기본 관리자 layout 정의

### 표준 오류 예시

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": [{ "field": "email", "reason": "INVALID_FORMAT" }],
  "traceId": "01J...",
  "timestamp": "2026-08-09T10:00:00Z"
}
```

### 완료 기준(DoD)

- 정상·검증 오류·업무 오류·서버 오류의 응답 규격이 테스트된다.
- migration으로 빈 DB를 최신 상태까지 생성할 수 있다.
- trace ID로 요청 로그를 추적할 수 있다.
- 목록 화면 공통 패턴이 샘플 API와 연결된다.

---

## Phase 2. 로그인, JWT, 세션 보안

### 목표

안전한 로그인·갱신·로그아웃 흐름과 보호된 관리자 shell을 완성한다.

### 작업 목록

- [ ] 사용자 비밀번호 해시 정책 구성(Argon2id 또는 BCrypt)
- [ ] login, refresh, logout, me API 구현
- [ ] 짧은 수명의 access token과 rotation되는 refresh token 구현
- [ ] refresh token hash 저장, 폐기, 만료, 재사용 탐지 정책 구현
- [ ] 로그인 실패 횟수 제한 및 계정 잠금 정책 구현
- [ ] CORS, CSRF, cookie 사용 여부를 배포 모델에 맞게 결정
- [ ] 프론트 로그인 화면, 인증 상태 복구, 만료 처리 구현
- [ ] 보호 route와 401/403 UX 구현
- [ ] 보안 이벤트 감사 로그 후보 정의

### 권장 API

```text
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
PUT    /api/v1/auth/password
```

### 완료 기준(DoD)

- 유효/무효/만료/변조 token 시나리오가 자동 테스트된다.
- 로그아웃 및 refresh token rotation 후 이전 token을 재사용할 수 없다.
- 인증되지 않은 사용자는 보호 화면과 API에 접근할 수 없다.
- 초기 관리자 계정 생성 방식이 운영 문서에 정의되어 있다.

---

## Phase 3. 사용자·역할·권한(RBAC)과 메뉴

### 목표

관리자가 사용자, 역할, 세부 권한과 메뉴 노출을 관리할 수 있게 한다.

### 작업 목록

- [ ] 사용자 목록/상세/생성/수정/활성화/잠금 해제
- [ ] 역할 CRUD와 사용자-역할 할당
- [ ] 권한 catalog와 역할-권한 할당
- [ ] 서버 method/API 권한 검사
- [ ] 메뉴 tree CRUD, 정렬, 활성화, route/icon metadata
- [ ] 역할-메뉴 연결 또는 권한 기반 메뉴 계산
- [ ] 프론트 sidebar 동적 구성과 action 단위 표시 제어
- [ ] 관리자 자기 자신의 마지막 최고 권한 제거 방지
- [ ] 권한 변경 후 token/permission cache 갱신 정책 구현

### 완료 기준(DoD)

- `SUPER_ADMIN`, `ADMIN`, `VIEWER` 대표 역할 시나리오가 통과한다.
- UI에서 숨겨진 action을 API로 직접 호출해도 403이 반환된다.
- 메뉴 tree의 순환, 잘못된 부모, 중복 정렬 문제를 검증한다.
- 최소 한 개 E2E 테스트가 역할별 화면/API 접근 차이를 확인한다.

---

## Phase 4. 파일 업로드 및 Local/S3 추상화

### 목표

동일한 애플리케이션 API로 Local 또는 S3/MinIO 저장소를 선택할 수 있게 한다.

### 작업 목록

- [ ] `FileStorage` port와 `LocalFileStorage`, `S3FileStorage` adapter 구현
- [ ] 파일 metadata, 상태, owner/reference 설계
- [ ] 허용 확장자/MIME/크기 검증과 안전한 서버 파일명 생성
- [ ] 업로드, 다운로드, 삭제, metadata 조회 API
- [ ] path traversal과 content-disposition 보안 처리
- [ ] DB 저장과 object 저장 간 실패 보상/정리 전략 구현
- [ ] orphan 파일 정리 job 설계
- [ ] 다중 파일 업로드 UI, 진행률, 오류 표시
- [ ] 게시판 첨부 연결 전에 독립 파일 관리 화면으로 검증

### 핵심 인터페이스 예시

```java
public interface FileStorage {
    StoredObject save(FileUploadCommand command);
    FileContent load(String storageKey);
    void delete(String storageKey);
}
```

### 완료 기준(DoD)

- 설정 변경만으로 Local과 MinIO 저장소 테스트가 모두 통과한다.
- 동일 파일명 충돌, 빈 파일, 제한 초과, 허용되지 않은 형식을 처리한다.
- 권한 없는 다운로드·삭제가 차단된다.
- object 저장 성공/DB 실패와 그 반대 상황에 대한 처리 방식이 테스트된다.

---

## Phase 5. 게시판과 첨부 파일

### 목표

검색·페이징·권한·첨부가 결합된 대표 업무 기능을 완성하여 공통 기반을 검증한다.

### 작업 목록

- [ ] 게시판 설정과 게시글 CRUD
- [ ] 제목/내용/작성자/기간 검색, 정렬, pagination
- [ ] 공개/비공개, 고정글, 게시 기간 정책
- [ ] 첨부 파일 연결과 삭제 정책
- [ ] 작성자/관리자 권한 분기
- [ ] optimistic locking 또는 충돌 감지
- [ ] 목록·상세·작성·수정 화면
- [ ] XSS 방지를 위한 HTML/Markdown 저장·렌더링 정책

### 완료 기준(DoD)

- 게시글 전체 사용자 흐름이 E2E로 통과한다.
- 검색 및 pagination 쿼리의 DB index와 실행 계획을 점검한다.
- 첨부 파일 소유권 및 게시글 삭제 연계가 테스트된다.
- 동시에 수정했을 때 데이터가 조용히 덮어써지지 않는다.

---

## Phase 6. Excel 가져오기/내보내기

### 목표

사용자와 게시글 데이터를 기준으로 대용량을 고려한 Excel 기능을 검증한다.

### 작업 목록

- [ ] 공통 workbook writer/reader와 컬럼 정의 계약 설계
- [ ] 사용자/게시글 export 구현
- [ ] 업로드 template 다운로드 구현
- [ ] 사용자 bulk import: 행별 검증, 오류 보고서 제공
- [ ] formula injection 방지
- [ ] 최대 행/파일 크기 및 처리 timeout 제한
- [ ] 대용량 streaming 또는 비동기 job 전환 기준 정의
- [ ] 프론트 다운로드/업로드/처리 결과 UX

### 완료 기준(DoD)

- 정상, 일부 행 오류, 전체 오류 파일이 각각 예측 가능하게 처리된다.
- 오류 보고서에 행 번호, 필드, 사유가 포함된다.
- CSV/Excel formula injection 위험 값이 안전하게 처리된다.
- 목표 데이터 규모에서 메모리와 처리 시간이 허용 범위에 든다.

---

## Phase 7. Audit Log

### 목표

누가 언제 무엇을 어떻게 변경했는지 민감 정보 없이 추적할 수 있게 한다.

### 작업 목록

- [ ] audit event schema와 기록 대상 행위 정의
- [ ] 로그인 성공/실패, 권한 변경, 사용자 변경, 파일 삭제 등 기록
- [ ] actor, action, resource, result, IP, user-agent, trace ID 저장
- [ ] before/after diff의 마스킹과 크기 제한
- [ ] 트랜잭션 연계 및 기록 실패 정책 결정
- [ ] 감사 로그 검색·상세 화면과 export
- [ ] 보존 기간, archive, 접근 권한 정의
- [ ] 위변조 완화 또는 append-only 운영 방안 문서화

### 완료 기준(DoD)

- 주요 보안·변경 이벤트가 누락 없이 기록된다.
- password, token, secret, 원본 파일 내용이 기록되지 않는다.
- 감사 로그 자체의 수정/삭제 권한이 기본적으로 제공되지 않는다.
- trace ID를 통해 API 로그와 감사 이벤트를 연결할 수 있다.

---

## Phase 8. 검증된 공통 기능의 모듈 추출

### 목표

동작을 바꾸지 않고 안정된 기능을 `common-*` 모듈로 옮기며 재사용성을 검증한다.

### 권장 추출 순서

1. `common-core`: 기반 타입, clock/ID, 오류 code
2. `common-web`: 오류 응답, pagination, trace
3. `common-security`: JWT, principal, 권한 검사 확장점
4. `common-file`: storage port와 Local adapter, `common-file-s3`: 선택적 S3 adapter
5. `common-audit`: audit event와 기록 port
6. `common-excel`: reader/writer와 검증 기반

### 작업 목록

- [ ] 추출 전 characterization/regression test 보강
- [ ] 앱 고유 DTO와 정책을 식별해 남김
- [ ] module public API 최소화
- [ ] Spring Boot auto-configuration 필요성 검토
- [ ] configuration properties와 기본값 문서화
- [ ] optional dependency 분리
- [ ] sample consumer 또는 별도 fixture app에서 적용 시험
- [ ] 모듈별 README와 사용 예제 작성
- [ ] dependency cycle/금지 의존 ArchUnit 검사
- [ ] semantic versioning 및 publishing 전략 정의

### 완료 기준(DoD)

- 추출 전후 동일한 통합/E2E 테스트가 통과한다.
- 공통 모듈이 `admin-server`의 controller/entity/DTO에 의존하지 않는다.
- 소비자가 필요한 설정과 확장 지점을 문서만으로 이해할 수 있다.
- 최소 한 개의 독립 test application에서 모듈 사용이 검증된다.

### Phase 8 경계 강화 실행 계획

현재 Gradle project 역의존은 차단되어 있고 독립 consumer도 존재하지만, public API와 선택 의존성 및 애플리케이션 패키지 경계는 아래 순서로 추가 강화한다. 각 단계는 별도 Issue로 추적하고 이전 단계의 필수 CI가 통과한 뒤 다음 단계로 진행한다.

#### 8.1 앱 오류 정책을 `common-core`에서 분리

**목표:** 공통 기반 타입이 사용자·역할·메뉴·Excel 같은 관리자 업무 정책과 한국어 메시지를 소유하지 않게 한다.

- [x] characterization test로 현재 오류 code/message와 HTTP 응답을 고정한다.
- [x] `ErrorCode`의 업무별 항목을 `admin-server`가 소유하는 오류 catalog로 이동한다.
- [x] `BusinessException`은 문자열 code/message 또는 최소 오류 descriptor 계약만 사용하게 하고 특정 enum 의존을 제거한다.
- [x] `ApiErrorFactory`가 앱 오류 enum을 알지 않도록 문자열 기반 생성 계약만 유지한다.
- [x] 독립 consumer에서 앱 오류 catalog의 부재와 최소 오류 descriptor 소비를 검증한다.
- [x] 전체 backend test와 `architectureTest` 및 Dev CI를 통과한다.

**완료 기준:** 기존 API 오류 응답 회귀 테스트가 유지되고, `common-core`와 `common-web`에 identity/menu/file/excel 업무 오류 상수가 없다.

#### 8.2 `common-excel`의 POI 비노출 계약 완성

**목표:** Apache POI를 구현 세부사항으로 격리하고 consumer compile classpath를 최소화한다.

- [x] POI `Row`를 받는 `ExcelCellSafety.rejectFormulas`의 실제 consumer가 없는 상태를 확인하고 제거한다.
- [x] `poi-ooxml` 의존성을 `api`에서 `implementation`으로 변경한다.
- [x] staged artifact만 사용하는 외부 consumer compile test에서 POI 없이 `XlsxTableReader`/`XlsxTableWriter`를 사용할 수 있음을 검증한다.
- [x] formula 거부, typed date, streaming export와 행 제한 회귀를 유지한다.
- [x] 전체 backend test와 `architectureTest` 및 Dev CI를 통과한다.

**완료 기준:** common-excel public signature에 `org.apache.poi.*`가 없고 생성 POM의 compile scope에 POI가 노출되지 않는다.

#### 8.3 파일 adapter 선택 의존성 분리

**목표:** Local 저장소만 사용하는 consumer가 AWS SDK를 내려받지 않게 한다.

- [x] `FileStorage` port와 Local adapter를 경량 core artifact에 유지한다.
- [x] S3 adapter, S3 properties와 client auto-configuration을 별도 선택 artifact로 분리한다.
- [x] Local-only, S3, consumer override 세 구성을 각각 context/contract test로 검증한다.
- [x] `admin-server`는 S3 기능이 필요하므로 두 artifact를 명시적으로 조립한다.
- [x] Local-only consumer의 compile/runtime dependency graph에 AWS SDK가 없음을 검증한다.
- [x] 전체 backend test와 `architectureTest` 및 Dev CI를 통과한다.

**완료 기준:** Local-only consumer의 compile/runtime dependency graph에 AWS SDK가 없고 Local/S3 저장 계약 테스트가 동일하게 통과한다.

#### 8.4 감사 계약에서 관리자 로그인 문맥 분리

**목표:** 감사 port가 servlet request attribute와 관리자 로그인 전용 필드를 공통 계약으로 강제하지 않게 한다.

- [x] 현재 로그인 성공 감사와 일반 변경 감사 결과를 characterization test로 고정한다.
- [x] `LOGIN_ACCOUNT_ATTRIBUTE`와 로그인 계정 전달 방식은 `admin-server` 웹 계층으로 이동한다.
- [x] 공통 감사 계약은 actor/action/resource/result/trace 등 저장소 독립적인 최소 이벤트만 유지하고, 필요한 actor snapshot 확장 방식은 명시적으로 정의한다.
- [x] 개인정보 필드는 allowlist, 길이 제한과 저장 목적이 검증된 경우에만 앱 adapter에서 구성한다.
- [x] 기존 감사 검색 데이터와 민감정보 회귀 테스트를 통과한다.
- [x] 전체 backend test와 `architectureTest` 및 Dev CI를 통과한다.

**완료 기준:** `common-audit`이 servlet·로그인 API 문맥을 알지 않고 기존 감사 검색 데이터와 민감정보 회귀 테스트가 통과한다.

#### 8.5 애플리케이션 package 경계 자동 검증

**목표:** Gradle project 검사만으로 놓치는 `admin-server` 내부 feature 간 결합과 계층 역참조를 CI에서 차단한다.

- [x] ArchUnit을 추가해 identity/menu/file 업무 패키지 간 직접 참조를 차단한다.
- [x] domain이 application/infrastructure/web/auth/config 외부 계층에 역참조하지 않는 규칙을 검사한다.
- [x] `architectureTest`에서 실제 ArchUnit 경계 테스트를 실행한다.
- [x] 의도적 위반 fixture로 RED를 확인한 뒤 예외 allowlist 없이 현행 production 경계를 GREEN으로 만든다.
- [x] 전체 backend test와 Dev CI를 통과한다.

**완료 기준:** 의도적인 위반 fixture가 RED가 되고, 전체 현행 코드가 규칙을 만족하도록 경계를 정리한 뒤 GREEN이 된다.

#### 8.6 배포 artifact 기준 소비 검증

**목표:** project dependency가 가려 주는 POM scope, 누락 resource와 자동설정 문제를 배포 전 발견한다.

- [x] repository-local staging에 발행한 JAR/POM만 사용하는 별도 Gradle fixture를 구성한다.
- [ ] 모듈별 최소 consumer와 전체 조합 consumer를 분리해 불필요한 전이 의존성을 검사한다.
- [ ] auto-configuration imports, configuration properties binding, bean override와 누락 설정 실패 메시지를 검증한다.
- [ ] public API 변경에는 binary/source compatibility 검사를 추가하고 semantic version 변경 기준과 연결한다.

**완료 기준:** project dependency 없이 모든 공통 artifact의 단독·조합 소비가 통과하고 POM scope 및 public API 호환성 검증이 Dev CI에 포함된다.

#### 실행 우선순위와 범위 제한

| 순서 | 작업 | 우선도 | 주요 위험 |
|---|---|---|---|
| 1 | 앱 오류 정책 분리 | P0 | 공개 오류 응답 회귀 |
| 2 | Excel POI 비노출 완성 | P0 | published POM의 불필요한 compile 의존 |
| 3 | package 경계 자동 검증 | P0 | 현재 숨은 feature 간 결합 발견 |
| 4 | 파일 S3 선택 artifact 분리 | P1 | auto-configuration 조합과 설정 호환성 |
| 5 | 감사 로그인 문맥 분리 | P1 | 기존 감사 데이터 의미 변경 |
| 6 | 배포 artifact 소비·호환성 검증 | P1 | CI 시간과 fixture 유지비 |

다음 항목은 실제 두 번째 사용처나 실패 사례가 생기기 전에는 진행하지 않는다.

- `common-security`를 JWT core와 Spring adapter로 세분화
- `common-web`의 일반 예외 handler를 자동설정으로 제공
- 공통 clock/ID abstraction 추가
- 업무 feature를 다시 별도 Gradle project로 분리

---

## Phase 9. CRUD 코드 생성기

### 목표

반복적인 CRUD 골격을 생성하되, 기존 코드를 안전하게 보호하고 생성 결과를 즉시 검증할 수 있게 한다.

### 범위

- 입력: DB metadata 또는 명시적 YAML/JSON schema
- 출력: migration 초안, entity, repository, service/use case, API DTO/controller, frontend list/form, 테스트 골격
- 사용자 검토가 필요한 이름, 권한 key, 관계, validation은 설정으로 명시

### 작업 목록

- [ ] generator schema와 template engine 선택
- [ ] dry-run과 생성 파일 diff 제공
- [ ] 기존 파일 overwrite 기본 금지
- [ ] 생성물 header/metadata와 template version 기록
- [ ] backend/frontend 생성 template 구현
- [ ] formatter 자동 적용
- [ ] sample domain 생성 후 compile/test
- [ ] 템플릿 사용자 정의 지점 문서화

### 완료 기준(DoD)

- 샘플 schema 하나로 CRUD vertical slice 골격을 생성할 수 있다.
- 생성 직후 backend compile과 frontend typecheck가 통과한다.
- 같은 명령 재실행이 사용자 코드를 파괴하지 않는다.
- dry-run 결과에서 생성/변경/충돌 파일을 구분할 수 있다.

---

## Phase 10. 릴리스, 운영 준비, Starter 검증

### 목표

MVP를 재현 가능하게 패키징하고 신규 프로젝트 도입 절차를 검증한다.

### 작업 목록

- [ ] production Dockerfile과 non-root 실행
- [ ] SBOM, dependency 취약점, secret scan
- [ ] health/readiness, graceful shutdown 설정
- [ ] DB backup/restore와 migration rollback 운영 절차
- [ ] 환경별 configuration/secrets 매트릭스 작성
- [ ] 이미지 tag 및 release note 자동화
- [ ] 모듈 artifact publishing 또는 composite build 전략 확정
- [ ] 빈 consumer project에서 설치 리허설
- [ ] 운영 runbook과 장애 대응 문서 작성

### 완료 기준(DoD)

- tag 기반으로 동일 artifact를 재생성·배포할 수 있다.
- staging smoke test와 핵심 E2E가 통과한다.
- critical/high 취약점 처리 기준을 충족한다.
- 새 consumer가 인증 또는 파일 모듈을 문서대로 적용할 수 있다.

---

## 7. MVP 범위와 후속 로드맵

### 7.1 MVP

MVP는 Phase 0~7의 실제 관리자 기능과 Phase 8의 핵심 모듈 추출까지로 정의한다.

- 로그인/refresh/logout
- 사용자·역할·권한 RBAC
- 권한 기반 메뉴
- Local/MinIO 파일 업로드·다운로드
- 게시판과 첨부
- 사용자 또는 게시글 Excel export/import
- 주요 행위 Audit Log
- `common-core`, `common-web`, `common-security`, `common-file`, `common-audit`
- Docker Compose 로컬 환경
- CI, OpenAPI, 핵심 E2E

코드 생성기는 MVP 직후 Phase 9에서 진행한다. 기능 경계와 표준 CRUD 패턴이 안정되기 전에 생성기를 만들면 잘못된 구조를 빠르게 복제할 위험이 있기 때문이다.

### 7.2 후속 로드맵

#### Release 1.1

- CRUD 코드 생성기
- 비동기 Excel job과 다운로드 보관함
- 파일 바이러스 검사 adapter
- 관리자 알림/이메일 기반 기능
- Storybook 또는 UI catalog

#### Release 1.2

- Redis 기반 rate limit/cache 선택 지원
- OIDC/OAuth2 adapter
- 세분화된 데이터 권한(row-level policy)
- object storage 추가 provider
- 감사 로그 archive와 외부 전송

#### Release 2.0 후보

- 멀티테넌시
- feature starter/BOM 발행
- 플러그인형 메뉴·기능 등록
- 이벤트/outbox 기반 외부 연동
- 서비스 분리가 필요한 영역의 선택적 추출

---

## 8. 데이터베이스 설계 및 구현 순서

### 8.1 원칙

- PostgreSQL 기준 `snake_case`를 사용한다.
- PK는 `bigint` 또는 UUIDv7/ULID 중 하나를 ADR로 선택한다.
- 모든 FK와 조회 조건에 필요한 index를 명시한다.
- unique constraint는 애플리케이션 validation과 별도로 DB에서 강제한다.
- migration은 수정하지 않고 새 migration으로 진화시킨다.
- 운영 데이터가 들어간 뒤 destructive migration은 expand-migrate-contract 절차를 따른다.

### 8.2 구현 순서

1. 기반: `users`, `refresh_tokens`
2. RBAC: `roles`, `permissions`, `user_roles`, `role_permissions`
3. 메뉴: `menus`, `role_menus` 또는 permission 연결
4. 파일: `files`, `file_references`
5. 게시판: `boards`, `posts`, 필요 시 `post_attachments`
6. 감사: `audit_logs`
7. Excel/비동기 확장 시: `jobs`, `job_results`

### 8.3 주요 테이블 초안

| 테이블 | 핵심 컬럼 |
|---|---|
| `users` | id, username, email, password_hash, status, failed_login_count, locked_until, version, timestamps |
| `refresh_tokens` | id, user_id, token_hash, family_id, expires_at, revoked_at, replaced_by |
| `roles` | id, code, name, description, system_role, timestamps |
| `permissions` | id, key, name, resource, action |
| `user_roles` | user_id, role_id |
| `role_permissions` | role_id, permission_id |
| `menus` | id, parent_id, name, route, icon, sort_order, permission_key, enabled |
| `files` | id, storage_type, storage_key, original_name, content_type, size, checksum, status, owner_id, timestamps |
| `file_references` | file_id, reference_type, reference_id, sort_order |
| `boards` | id, code, name, settings_json, enabled |
| `posts` | id, board_id, title, content, author_id, status, pinned, publish_from, publish_to, version, timestamps |
| `audit_logs` | id, occurred_at, actor_id, action, resource_type, resource_id, result, trace_id, ip, user_agent, changes_json |

### 8.4 Seed 전략

- 개발 환경 seed와 운영 bootstrap을 분리한다.
- 초기 관리자 비밀번호를 migration에 평문/고정 hash로 넣지 않는다.
- 운영에서는 one-time bootstrap command 또는 환경 secret을 사용하고 최초 로그인 시 변경하게 한다.
- 권한 catalog는 코드/설정 기반 동기화와 DB 수동 관리 중 하나를 명확히 선택한다.

---

## 9. API 설계 기준

### 9.1 규칙

- 리소스는 복수 명사: `/users`, `/roles`.
- 검색은 query parameter, 복잡한 검색은 명시적 `/search` endpoint를 검토한다.
- pagination은 `page`, `size`, `sort`로 시작하되 대용량은 cursor 방식으로 확장한다.
- 생성은 `201 Created`, 삭제 성공은 `204 No Content`를 기본으로 한다.
- 낙관적 잠금 충돌은 `409 Conflict`로 반환한다.
- validation과 업무 오류는 안정적인 machine-readable `code`를 갖는다.
- bulk 작업은 부분 성공 정책을 API 문서에 명시한다.

### 9.2 대표 API 목록

```text
/api/v1/auth/*
/api/v1/users
/api/v1/roles
/api/v1/permissions
/api/v1/menus
/api/v1/files
/api/v1/boards
/api/v1/boards/{boardId}/posts
/api/v1/audit-logs
/api/v1/excel/*
```

### 9.3 Contract 관리

- OpenAPI를 API 계약의 배포 가능한 산출물로 취급한다.
- 프론트 client 생성 시 생성 코드와 수동 코드를 분리한다.
- breaking change는 major version 또는 호환 기간을 둔다.
- 오류 코드 catalog를 `docs/api/error-codes.md`에 유지한다.

---

## 10. 테스트 전략

### 10.1 테스트 피라미드

| 계층 | 대상 | 도구/방식 | 실행 시점 |
|---|---|---|---|
| Unit | 정책, validator, mapper, token/file/excel 유틸 | JUnit/Vitest | 매 커밋 |
| Slice | Controller, JPA repository, security filter | Spring test slices | PR |
| Integration | PostgreSQL, MinIO, migration, JWT, storage | Testcontainers | PR |
| Contract | OpenAPI와 API client 호환 | schema diff/typecheck | PR |
| Component UI | form, table, permission rendering | RTL/Vitest | PR |
| E2E | login, RBAC, file, post, Excel | Playwright | PR 핵심, nightly 전체 |
| Security | dependency/secret/SAST 및 권한 회귀 | CI scanners + tests | PR/release |
| Performance | 목록, Excel, 업로드, login | k6/JMeter 선택 | release 전 |

### 10.2 필수 회귀 시나리오

- access token 만료 후 안전한 refresh 및 재시도
- refresh token 재사용 탐지와 세션 폐기
- Viewer가 변경 API를 직접 호출할 때 403
- 마지막 최고 관리자 권한 제거 방지
- Local/MinIO 저장소 동일 계약 테스트
- 파일 저장과 DB transaction 실패 보상
- 게시글 동시 수정 충돌
- Excel 일부 오류와 오류 보고서
- 민감 정보가 Audit Log에 남지 않음
- migration을 빈 DB와 이전 릴리스 DB 양쪽에 적용

### 10.3 테스트 데이터

- builder/fixture를 사용하고 테스트 간 공유 상태를 피한다.
- 운영 데이터 덤프를 테스트에 사용하지 않는다.
- E2E seed는 결정적이며 재실행 가능해야 한다.
- 시간 의존 로직은 `Clock`을 주입하여 고정한다.

### 10.4 Phase 공통 완료 확인

각 Phase는 테스트를 구현보다 먼저 작성하고, 변경 범위에 맞는 GitHub Actions 검증을 완료해야 한다. `dev` 변경은 `Dev CI`, `infra/**` 변경은 `Dev Infra CI`, `dev → main` 배치 PR은 `Main Integration CI`가 최종 판정 기준이다. 로컬 명령은 디버깅 또는 단일 테스트 확인이 필요한 경우에만 지정한다. Actions를 사용할 수 없으면 미검증 상태로 보고하고 병합하지 않는다.

---

## 11. Docker 기반 로컬 개발환경

### 11.1 서비스

```text
postgres   필수 데이터베이스
minio      S3 호환 파일 저장 검증
mailpit    향후 메일 기능용, 선택
redis      MVP 이후 cache/rate limit용, 선택
```

### 11.2 원칙

- host port와 container 내부 주소를 구분해 문서화한다.
- service healthcheck와 dependency readiness를 설정한다.
- DB/MinIO 초기화 스크립트는 재실행 가능해야 한다.
- volume 삭제 없이 일반 restart가 가능해야 한다.
- 데이터 초기화는 명시적 별도 명령으로 제공한다.
- ARM64/x86_64 개발 환경을 고려한다.

### 11.3 권장 개발 흐름

1. 환경 파일 복사 및 로컬 값 설정
2. PostgreSQL/MinIO 시작
3. backend 실행 및 Flyway 적용
4. frontend 실행
5. seed/bootstrap 명령으로 관리자 생성
6. smoke test 실행

---

## 12. CI/CD 실행 계획

### 12.1 Pull Request CI

- `Dev CI`: `dev` push와 `dev` 대상 PR에서 backend `test architectureTest`, frontend `lint`, `typecheck`, `test`를 실행한다.
- `Dev Infra CI`: `dev` push와 `dev` 대상 PR의 `infra/**` 변경에서 `docker compose -f infra/compose.yaml config`를 실행한다.
- 같은 ref에 새 커밋이 오면 이전 빠른 CI를 취소한다.
- 빠른 CI 실패는 다음 이슈로 진행하기 전에 같은 이슈에서 수정한다.

### 12.2 Main/Release Pipeline

`Main Integration CI`는 `main` 대상 PR과 수동 실행에서 backend `clean test integrationTest architectureTest`, frontend `lint`, `typecheck`, `test`, `build`, Playwright E2E, Docker Compose 설정 검증을 실행한다. `main` 병합은 이 필수 checks가 모두 통과한 `dev → main` 배치 PR에서만 한다.

### 12.3 배포 원칙

- 동일 이미지를 staging에서 검증 후 production으로 승격한다.
- secret은 image와 repository에 포함하지 않는다.
- DB migration은 하위 호환 가능한 순서로 배포한다.
- 실패 시 앱 rollback과 DB forward-fix 절차를 구분한다.
- 배포 후 login, me, 권한 조회, file health를 smoke test한다.

---

## 13. 보안 체크리스트

### 인증/세션

- [ ] 강한 password hashing과 정책 적용
- [ ] access token 짧은 만료, refresh rotation 및 폐기
- [ ] JWT algorithm/key/issuer/audience 검증
- [ ] login rate limit과 brute-force 완화
- [ ] 계정 존재 여부를 노출하지 않는 오류
- [ ] 로그아웃 및 비밀번호 변경 시 세션 폐기 정책

### 권한

- [ ] 모든 변경 API에 서버 권한 검사
- [ ] 객체 단위 소유권/접근권한 검사
- [ ] 기본 거부(default deny)
- [ ] 최고 관리자 보호와 권한 상승 경로 테스트
- [ ] 메뉴 숨김을 보안 통제로 오해하지 않음

### 입력/출력

- [ ] Bean Validation과 길이/범위 제한
- [ ] XSS 방지 및 HTML sanitization 정책
- [ ] SQL은 parameter binding 사용
- [ ] Excel formula injection 방지
- [ ] Open redirect와 잘못된 URL 입력 방지

### 파일

- [ ] MIME, 확장자, magic bytes, 크기 검사
- [ ] 사용자 파일명으로 실제 저장 경로를 만들지 않음
- [ ] path traversal 차단
- [ ] executable/script 업로드 정책
- [ ] 다운로드 권한과 안전한 response header
- [ ] 향후 malware scanner 연동 지점 확보

### 운영/공급망

- [ ] secret scan과 dependency scan
- [ ] 최소 권한 DB/object storage credentials
- [ ] production debug/Swagger 노출 정책
- [ ] 보안 header, TLS, CORS 설정
- [ ] 로그 및 Audit의 개인정보 마스킹
- [ ] backup 암호화와 restore 훈련
- [ ] 컨테이너 non-root, read-only filesystem 가능성 검토

---

## 14. 공통 모듈 분리 기준

### 14.1 분리 점수표

아래 항목 중 5개 이상이 명확히 충족될 때 추출을 우선 검토한다.

- [ ] 두 개 이상의 feature 또는 consumer가 필요로 한다.
- [ ] 프로젝트 고유 UI/API DTO와 분리 가능하다.
- [ ] 안정된 public interface를 설명할 수 있다.
- [ ] 독립 unit/contract test가 가능하다.
- [ ] 설정과 확장 지점이 명확하다.
- [ ] optional dependency를 격리할 수 있다.
- [ ] 추출로 순환 의존이 생기지 않는다.
- [ ] 향후 변경 주기가 consumer와 다르다.

### 14.2 모듈에 남길 것과 앱에 남길 것

| reusable module | admin app/feature |
|---|---|
| storage/security/audit port | Controller와 route |
| 범용 adapter와 validator | 화면/API 전용 DTO |
| configuration properties | 조직별 업무 정책 |
| 공통 오류와 확장점 | 메뉴 구성과 UX |
| contract test kit | 실제 permission 조합 |

### 14.3 분리 금지 신호

- 이름만 `common`이고 서로 무관한 유틸이 계속 모인다.
- 범용화를 위해 지나치게 많은 generic/flag/callback이 필요하다.
- 한 곳에서만 사용되며 변경이 잦다.
- 모듈을 쓰려면 앱 내부 entity를 그대로 가져와야 한다.
- auto-configuration이 consumer 설정을 예측하기 어렵게 만든다.

---

## 15. Git 브랜치, 커밋, PR 규칙

세부 운영 절차와 자동 검증 규칙은 [`docs/development/branch-strategy.md`](../docs/development/branch-strategy.md)를 기준으로 한다.

### 15.1 브랜치

- 기본 브랜치: `main`, 항상 배포 가능 상태 유지
- 장기 dev 통합 브랜치: `dev`, 완료된 이슈의 논리적 커밋을 누적한다.
- 작은 기능, 격리된 버그, 테스트, 문서는 `dev`에 직접 커밋한다.
- migration, 보안, 공개 API/공용 설정, 장기·병렬·고위험 작업은 feature branch를 사용하고 로컬 검증 후 PR 없이 `dev`에 merge commit으로 병합한다.
- 사람 브랜치 형식: `<type>/<issue-number>-<slug>`, Codex 브랜치 형식: `codex/<issue-number>-<slug>`
- 최초 정책 부트스트랩 예외는 브랜치 전략 문서의 신뢰 경계를 따르며, feature branch는 PR 없이 `dev`로 merge commit 병합하고 `main`은 `dev → main` 배치 PR만 받는다.

### 15.2 커밋 규칙

커밋과 PR 제목은 [`docs/development/commit-convention.md`](../docs/development/commit-convention.md)의 `type: 한글 변경사항` 형식을 사용한다.

- 한 커밋은 하나의 논리적 변경만 포함한다.
- 생성 파일과 수동 변경은 가능하면 분리한다.
- schema 변경은 migration과 관련 테스트를 같은 PR에 포함한다.
- 깨진 빌드, 미완성 placeholder, 실제 secret을 커밋하지 않는다.
- 일반 커밋은 `Refs: #123`, 배치 PR은 포함 이슈마다 `Closes #123`으로 연결한다.

### 15.3 PR 체크리스트

- [ ] 요구사항과 제외 범위가 명확하다.
- [ ] 테스트를 구현보다 먼저 작성했다(test-first).
- [ ] `dev` 변경은 `Dev CI`를 통과했고, `infra/**` 변경은 `Dev Infra CI`도 통과했다.
- [ ] `dev → main` 배치 PR은 `Main Integration CI`를 통과해 CI 검증 완료 상태다.
- [ ] Actions를 사용할 수 없으면 미검증 상태로 보고하고 병합하지 않는다.
- [ ] API/DB/설정 변경 문서가 갱신되었다.
- [ ] 보안·권한·개인정보 영향을 검토했다.
- [ ] migration의 forward/compatibility를 확인했다.
- [ ] 공통 모듈 의존 방향을 위반하지 않는다.
- [ ] UI의 loading/empty/error/권한 없음 상태를 확인했다.
- [ ] rollback 또는 실패 복구 방법이 설명되어 있다.

---

## 16. Codex 작업 운영 규칙

### 16.1 작업 단위

Codex에는 한 번에 하나의 검증 가능한 목표를 준다. 한 요청에 전체 Phase를 구현시키지 않고, 보통 다음 크기로 나눈다.

1. 현황 조사와 변경 계획
2. schema/migration
3. backend domain/use case
4. API와 보안
5. frontend UI
6. integration/E2E
7. 문서와 최종 검증

각 작업은 수정 허용 범위, 비범위, 완료 기준, test-first 작성 여부와 필요한 GitHub Actions check를 포함해야 한다. 작은 작업은 `dev`에 직접 누적하고, 고위험·장기·병렬 작업만 feature branch에서 `dev`로 전달한다.

### 16.2 Codex 공통 프롬프트 템플릿

```text
목표:
- [달성할 사용자 관점 결과]

현재 구조/관련 파일:
- [모듈과 파일]

요구사항:
1. ...
2. ...

비범위:
- 이번 작업에서 하지 않을 것

제약:
- 모듈 의존 방향, API 규약, 보안 규칙
- 기존 사용자 변경을 보존할 것

완료 기준:
- 동작/테스트/문서 기준

검증:
- test-first 작성 여부와 필요한 GitHub Actions check
- 로컬 명령은 디버깅 또는 단일 테스트 확인이 필요한 경우에만 지정

먼저 관련 파일과 AGENTS.md를 읽고 짧은 변경 계획을 세운 뒤 구현하라.
완료 후 변경 파일, 핵심 결정, `CI 검증 완료` 여부, 로컬 검증 결과, 남은 위험을 보고하라. Actions를 사용할 수 없으면 미검증으로 보고하고 병합하지 마라.
```

### 16.3 Phase별 예시 프롬프트

#### JWT 작업

```text
Phase 2의 refresh token rotation을 구현해줘. 현재 login 흐름과 DB migration을 먼저 확인하고,
token 원문은 저장하지 말며 family 단위 재사용 탐지를 지원해. login/refresh/logout API 계약은
PROJECT_PLAN.md를 따르고, 유효·만료·폐기·재사용 시나리오 통합 테스트를 추가해.
이번 작업에서는 프론트 UI를 변경하지 마. 완료 후 migration과 테스트 결과를 요약해.
```

#### 모듈 추출 작업

```text
현재 Local/S3 파일 기능을 common-file 모듈로 추출해줘. 동작 변경 없는 리팩터링이며,
Controller와 관리자 API DTO는 admin-server에 남겨. 먼저 characterization test를 보강하고,
FileStorage port와 adapter/configuration만 이동해. admin-server 역의존과 순환 의존을 검사하고,
기존 파일 E2E가 그대로 통과하는지 확인해.
```

#### 보안 리뷰 작업

```text
파일 업로드 구현을 보안 관점에서 리뷰해줘. 경로 탐색, MIME/확장자 불일치, 크기 제한,
권한 없는 다운로드, content-disposition, 저장-DB 실패 보상을 확인해. 먼저 발견 사항을
심각도와 파일/라인 근거로 보고하고, 내가 요청하기 전에는 코드를 수정하지 마.
```

### 16.4 Codex 완료 보고 형식

```text
- 결과: 사용자 관점에서 무엇이 동작하는가
- 변경: 핵심 파일/모듈과 설계 결정
- 검증: 실행한 테스트와 결과
- 주의: 미검증 항목, migration/운영 영향, 후속 작업
```

---

## 17. AGENTS.md 활용 규칙

루트 `AGENTS.md`에는 저장소 전체에 적용되는 불변 규칙만 둔다. 특정 모듈 규칙은 해당 디렉터리의 더 가까운 `AGENTS.md`에 둔다.

### 17.1 루트 AGENTS.md 권장 내용

```markdown
# Repository Instructions

## Architecture
- Respect dependency direction documented in PROJECT_PLAN.md.
- Do not expose JPA entities directly through APIs.
- Keep app-specific controllers and DTOs out of common modules.

## Workflow
- Read the nearest AGENTS.md before editing.
- Preserve unrelated user changes.
- Add or update tests with behavior changes.
- Do not edit applied Flyway migrations; add a new migration.

## Verification
- Backend: run the affected module tests and architecture tests.
- Frontend: run lint, typecheck, unit tests, and build for UI changes.
- Report commands run and any checks not run.

## Security
- Never commit secrets or log passwords/tokens.
- Enforce authorization on the server.
- Treat uploads and spreadsheet cells as untrusted input.
```

### 17.2 하위 AGENTS.md 예시

- `apps/admin-web/AGENTS.md`: shadcn 사용 규칙, query/form 패턴, 접근성, UI 테스트
- `modules/common-security/AGENTS.md`: token/crypto 금지사항, 필수 보안 테스트
- `modules/common-file/AGENTS.md`: 저장소 contract, path/MIME 검증, adapter 테스트
- `infra/AGENTS.md`: secret 처리, destructive operation, 배포 검증

### 17.3 관리 원칙

- 구체적이고 검증 가능한 문장으로 작성한다.
- 일시적인 작업 지시는 issue/프롬프트에 두고 AGENTS.md에 쌓지 않는다.
- 명령이 바뀌면 CI와 AGENTS.md를 함께 갱신한다.
- 충돌하는 규칙을 만들지 말고, 하위 규칙은 해당 범위의 차이만 적는다.

---

## 18. Definition of Done 공통 기준

모든 기능/Phase는 아래 기준을 충족해야 완료로 본다.

### 기능

- [ ] acceptance criteria가 실제 사용자 흐름에서 동작한다.
- [ ] 실패/빈 상태/권한 없음/재시도 UX가 정의되어 있다.
- [ ] API와 UI의 권한 결과가 일관된다.

### 코드와 아키텍처

- [ ] 모듈 책임과 의존 방향을 지킨다.
- [ ] 공개 계약이 최소화되고 명명 규칙이 일관된다.
- [ ] 임시 TODO는 issue 없이 남기지 않는다.

### 데이터

- [ ] migration, constraint, index, rollback/forward-fix 영향을 검토했다.
- [ ] 시간대, 동시성, 삭제 정책을 고려했다.

### 테스트

- [ ] unit/integration/UI 테스트가 변경 위험에 맞게 추가되었다.
- [ ] 구현 전에 테스트를 작성했고, 필요한 GitHub Actions checks가 통과했다.
- [ ] 핵심 회귀와 관련 E2E가 필요한 `Main Integration CI`에서 통과한다.
- [ ] flaky test가 없다.

### 보안과 운영

- [ ] 인증/인가, 입력 검증, 로그 마스킹을 확인했다.
- [ ] 설정, metric/log, 장애 시 복구 방법이 준비되었다.
- [ ] secret이나 실제 개인정보가 저장소/fixture에 없다.

### 문서

- [ ] README/OpenAPI/ADR/운영 문서 중 영향받는 문서를 갱신했다.
- [ ] 신규 설정의 기본값과 예시가 있다.
- [ ] 완료 보고에 검증 결과와 남은 위험이 포함된다.

---

## 19. 리스크와 대응

| 리스크 | 영향 | 대응 |
|---|---|---|
| 너무 이른 모듈화 | 복잡한 추상화와 느린 개발 | 앱에서 vertical slice 검증 후 점수표로 추출 |
| JWT 폐기 어려움 | 탈취 token 지속 사용 | 짧은 access token, refresh 저장/rotation/reuse detection |
| 메뉴와 권한 혼동 | UI 숨김만으로 보안 처리 | 서버 권한 강제, 메뉴는 표현 계층으로 분리 |
| 파일과 DB 불일치 | orphan 또는 유실 | 상태 모델, 보상 처리, cleanup job |
| 대용량 Excel 메모리 | 장애 및 timeout | streaming, 크기 제한, 비동기 전환 기준 |
| Audit에 민감 정보 저장 | 보안/규제 문제 | allowlist, masking, 크기 제한, 전용 테스트 |
| 코드 생성기가 기존 코드 파괴 | 사용자 변경 유실 | dry-run, overwrite 금지, diff, 생성 영역 분리 |
| 공통 모듈 의존성 팽창 | consumer 도입 부담 | optional adapter 분리, 최소 public API |
| E2E 불안정 | CI 신뢰 저하 | 결정적 seed, 격리, 핵심/전체 suite 분리 |

---

## 20. 초기 백로그 우선순위

다음 순서로 issue를 생성하고 한 항목씩 완료한다.

1. 저장소/빌드/Compose skeleton
2. backend health + PostgreSQL migration
3. React admin shell + API 연결
4. 공통 오류/validation/trace ID
5. 사용자 테이블과 관리자 bootstrap
6. login + access token
7. refresh rotation + logout
8. 사용자 관리 vertical slice
9. 역할/권한과 서버 authorization
10. 동적 메뉴와 frontend permission UX
11. Local 파일 저장 vertical slice
12. S3/MinIO adapter와 contract test
13. 게시판 + 첨부 파일 E2E
14. Excel export/import
15. Audit Log
16. 공통 모듈 characterization tests
17. `common-core`/`common-web` 추출
18. `common-security`/`common-file`/`common-audit` 추출
19. 독립 consumer 검증
20. 코드 생성기와 release pipeline

---

## 21. Phase Gate 체크포인트

각 Phase 종료 시 다음 질문에 답하고 결과를 ADR, issue 또는 PR에 남긴다.

1. 사용자가 끝까지 수행할 수 있는 흐름이 무엇인가?
2. 자동 테스트가 증명하는 성공/실패/권한 시나리오는 무엇인가?
3. 새로 생긴 설정, 운영 부담, 보안 위험은 무엇인가?
4. 다음 Phase 전에 반드시 해결할 기술 부채가 있는가?
5. 현재 인터페이스는 추출할 만큼 안정적인가, 아니면 앱에 더 남겨야 하는가?
6. 문서만으로 다른 개발자나 Codex가 다음 작업을 시작할 수 있는가?

Gate를 통과하지 못한 기능은 다음 Phase 범위를 늘려 덮지 않고 현재 Phase에서 수정한다.

---

## 22. 첫 번째 실행 목표

첫 번째 마일스톤은 **Phase 0 + Phase 1의 최소 vertical slice**다.

완료 시 다음이 가능해야 한다.

```text
Docker Compose로 PostgreSQL/MinIO 실행
→ admin-server 시작 및 migration 적용
→ admin-web 시작
→ 웹에서 backend health/sample 목록 호출
→ 오류/trace ID 확인
→ backend/frontend CI 통과
```

이 상태를 만든 다음 로그인부터 한 Phase씩 진행한다. 모듈 디렉터리는 미리 만들 수 있지만, 검증되지 않은 구현을 억지로 채우지 않는다. 프로젝트의 첫 번째 중요한 산출물은 많은 `common-*` 코드가 아니라, 작고 확실하게 동작하는 관리자 애플리케이션이다.
