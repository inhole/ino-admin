# 운영 UX와 실시간 관제 개선 설계

## 1. 목적

Milestone #4는 운영자가 관리자 시스템을 안정적으로 시작하고, 사용자를 더 편하게 생성하며, 별도 관제 인프라 없이 현재 애플리케이션 상태를 확인할 수 있게 한다.

작업은 다음 세 Issue로 나눈다.

- Issue #57: 초기 관리자 한글 표시 이름 보존
- Issue #58: 사용자 생성 모달 UX 도입
- Issue #59: 실시간 애플리케이션 관제 대시보드 도입

각 Issue는 독립적으로 테스트하고 검증한다. 앞 Issue의 필수 GitHub Actions가 통과하기 전에는 다음 Issue를 시작하지 않는다.

## 2. 범위와 비범위

### 범위

- UTF-8 한글 초기 관리자 표시 이름의 설정 바인딩과 PostgreSQL 저장·조회 보존
- 사용자 목록의 상시 생성 폼을 권한 기반 모달로 분리
- CPU, JVM heap, uptime, thread와 HTTP TPS, 평균 응답시간, 5xx 오류율 관제
- 5초 간격 snapshot 조회와 브라우저 세션의 최근 30분 차트
- `monitoring:read` 서버 인가

### 비범위

- 이미 깨져 저장된 표시 이름의 자동 추측·일괄 변환
- Prometheus, Grafana 또는 별도 시계열 저장소 도입
- 서버 재시작이나 브라우저 재접속을 넘는 메트릭 이력 보존
- 알림 규칙, 임계치 통보, 분산 추적, 로그 검색
- 사용자 생성 API 계약의 불필요한 변경

## 3. 초기 관리자 한글 보존

기존 `AdminBootstrapProperties → AdminBootstrapRunner → AdminBootstrapService → UserRepository` 흐름을 유지한다. 먼저 설정 파일 기본값과 환경 변수 값을 바인딩한 문자열이 UTF-8 한글을 보존하는지 확인하고, 실제 PostgreSQL 저장·조회까지 왕복 테스트한다.

깨진 문자열을 휴리스틱으로 감지하거나 다른 문자셋으로 역변환하지 않는다. 그런 보정은 정상 문자열을 손상시킬 수 있기 때문이다. 입력 경계의 실제 원인을 고치고 `.env`, PowerShell 환경 변수와 컨테이너 환경에서 UTF-8 값을 전달하는 방법을 운영 문서에 명시한다.

기존 손상 계정은 사용자 관리 화면 또는 명시적인 SQL 정정 절차로 수정한다. 자동 migration은 만들지 않는다.

## 4. 사용자 생성 모달

`UsersPage`는 목록 조회, 필터, 페이지 이동과 사용자 수정 흐름을 유지한다. 생성 폼과 mutation 상태는 별도 `CreateUserDialog` 컴포넌트로 이동한다.

`user:create` 권한이 있는 사용자에게만 페이지 헤더 근처의 `사용자 추가` 버튼을 표시한다. 버튼을 누르면 이름, 이메일, 초기 비밀번호, 역할 필드를 가진 모달이 열린다. 모달은 다음 동작을 제공한다.

- 열릴 때 첫 필드로 포커스 이동
- 제목과 설명을 보조 기술에 연결
- Escape와 취소 버튼으로 닫기
- 제출 중 중복 요청 방지와 닫기 제한
- 필드 기본 검증과 서버 오류 표시
- 성공 시 모달 닫기, 폼 초기화, 사용자 query 무효화, 성공 toast
- 닫은 뒤 trigger 버튼으로 포커스 복귀

버튼 숨김은 UX일 뿐이며 보안 통제가 아니다. 기존 서버의 `user:create` 인가를 유지하고 직접 API 호출의 403 회귀 테스트를 보존한다.

## 5. 관제 API

서버는 Micrometer `MeterRegistry`를 읽어 `/api/v1/monitoring/summary`에서 한 시점의 snapshot DTO를 반환한다. Actuator metrics 원본 엔드포인트는 공개하지 않는다. 공개 API는 화면에 필요한 최소 계약만 제공한다.

snapshot은 다음 값을 포함한다.

- 서버 측 UTC timestamp
- system/process CPU 사용률
- JVM heap 사용량과 최대량
- process uptime
- live/peak thread 수
- 누적 HTTP 요청 수
- 누적 HTTP 처리 시간
- 누적 HTTP 5xx 요청 수

Micrometer meter가 실행 환경에 없으면 해당 값만 `null`로 반환하고 전체 요청은 성공시킨다. 카운터가 없거나 0이면 유효한 0과 수집 불가를 구분한다. 비밀번호, token, URL parameter, 사용자 식별자 같은 민감 정보나 고카디널리티 tag는 응답하지 않는다.

API는 `monitoring:read` 권한으로 보호한다. 기본 역할 중 `SUPER_ADMIN`과 `ADMIN`에 권한을 부여하고 `VIEWER`에는 부여하지 않는다. 권한 catalog와 migration은 기존 Flyway 정책에 따라 새 migration으로 변경한다.

## 6. 대시보드 데이터 흐름과 차트

웹은 5초마다 snapshot을 조회한다. React Query는 브라우저 탭이 백그라운드일 때 polling을 중단하고 다시 활성화되면 최신 snapshot부터 수집한다. 메모리에는 최대 360개 snapshot만 유지하여 약 30분의 이력을 만든다.

TPS, 구간 평균 응답시간과 5xx 오류율은 연속한 두 snapshot의 누적값 차이와 timestamp 차이로 계산한다.

- TPS = 요청 수 증가량 / 경과 초
- 평균 응답시간 = 처리 시간 증가량 / 요청 수 증가량
- 5xx 오류율 = 5xx 증가량 / 요청 수 증가량 × 100

첫 snapshot, counter reset, 음수 증가량, 0초 간격 또는 요청 증가량 0은 안전하게 처리한다. 첫 snapshot은 파생 지표를 `수집 중`으로 표시한다. 애플리케이션 재시작으로 counter가 감소하면 기존 구간과 연결하지 않고 새 기준점으로 취급한다.

현재값 카드는 CPU, heap, uptime, thread, TPS, 평균 응답시간, 오류율을 요약한다. Recharts 기반 차트는 단위가 다른 지표를 억지로 한 축에 합치지 않고 다음처럼 분리한다.

- CPU 사용률
- heap 사용량과 최대량
- TPS
- 평균 응답시간
- 5xx 오류율

초기 수집, 일부 meter 누락, 전체 요청 오류와 재시도 상태를 명확히 구분한다. 차트는 색상만으로 계열을 구분하지 않고 label, legend, tooltip과 접근 가능한 요약값을 제공한다.

## 7. 오류 처리

- 초기 관리자 설정이 유효하지 않으면 secret이나 원문을 로그에 남기지 않고 시작을 실패시킨다.
- 사용자 생성 검증 오류는 모달 안에 유지하며 입력값을 보존한다.
- 관제 API에서 개별 meter 누락은 nullable field로 격리한다.
- 관제 API 자체가 실패하면 마지막 성공 snapshot을 조작하지 않고 오류와 재시도 동작을 표시한다.
- 401은 기존 인증 복구 흐름을 따르고 403은 권한 없음 상태로 표시한다.

## 8. 테스트와 검증

### Issue #57

- configuration binding에서 기본값과 환경 입력의 한글 보존
- PostgreSQL persistence integration test의 한글 왕복
- 운영 문서의 UTF-8 실행 예시

### Issue #58

- 권한별 trigger 표시
- 모달 열기·닫기·포커스 복귀
- 제출 중 상태와 중복 방지
- 검증 오류와 서버 오류
- 성공 후 닫기, query 무효화와 알림
- 서버 `user:create` 403 회귀

### Issue #59

- `monitoring:read` 401/403/정상 응답
- MeterRegistry 정상값, 누락값과 0분모
- snapshot 차이 계산, counter reset과 최대 360개 제한
- polling의 수집 중·일부 누락·오류·재시도 UI
- backend 관련 테스트
- frontend lint, typecheck, test, build

동작 변경은 테스트를 먼저 작성한다. 각 Issue는 `dev` 또는 `dev`에서 시작한 `codex/<issue>-<slug>` 브랜치에서 저장소 위험 기준에 맞춰 진행한다. Issue #59는 migration과 보안 경계를 포함하므로 feature 브랜치를 사용해 `dev` 대상 PR로 전달한다. 완료는 필요한 GitHub Actions 통과를 직접 확인한 뒤에만 보고한다.

## 9. 운영 영향과 후속 확장

이번 설계는 단일 인스턴스의 현재 상태를 빠르게 확인하는 도구다. 여러 인스턴스의 통합 이력, 장기 보존, 경보가 필요해지면 Prometheus/Grafana 같은 외부 관제 체계를 별도 Issue로 도입한다. 이때 현재 대시보드 API를 시계열 저장소 계약으로 확장하기보다 운영 대시보드의 목적과 데이터 소스를 다시 설계한다.
