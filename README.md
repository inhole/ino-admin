# INO Admin

Spring Boot 4.1 / Java 25 / React 기반 관리자 starter입니다. 현재 범위는 PostgreSQL migration, health endpoint, trace ID와 표준 오류 응답, 샘플 목록을 표시하는 관리자 shell입니다.

## 요구 사항

- Java 25 LTS
- Node.js 22 이상과 npm
- Docker Desktop 또는 Docker Engine + Compose

## 로컬 실행

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f infra/compose.yaml up -d
$jwtBytes = New-Object byte[] 32
$jwtRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$jwtRng.Dispose()
$env:APP_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
./gradlew.bat :apps:admin-server:bootRun
```

다른 터미널에서 웹을 시작합니다.

```powershell
Set-Location apps/admin-web
npm install
npm run dev
```

웹의 `/login`에서 초기 관리자 계정으로 로그인할 수 있습니다. 인증 token은 현재 브라우저 탭의 `sessionStorage`에만 유지되며 새로고침 시 refresh token rotation으로 인증 상태를 복구합니다. 탭을 닫거나 로그아웃하면 다시 로그인해야 합니다.

- 웹: http://localhost:5173
- backend health: http://localhost:8080/actuator/health
- OpenAPI UI: http://localhost:8080/swagger-ui.html
- MinIO console: http://localhost:9001

`application.yml`의 로컬 기본값은 `.env.example`과 일치합니다. 공유 환경에서는 `DB_PASSWORD` 및 MinIO 자격 증명을 반드시 별도로 설정하십시오.

## 초기 관리자 생성

초기 관리자 계정은 migration에 포함되지 않습니다. 서버를 최초 한 번 실행할 때만 환경 변수로 bootstrap을 활성화합니다.

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED='true'
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='<강한 임시 비밀번호>'
$env:APP_BOOTSTRAP_ADMIN_DISPLAY_NAME='시스템 관리자'
./gradlew.bat :apps:admin-server:bootRun
```

비밀번호는 12~128자이며 대문자·소문자·숫자·특수문자를 포함해야 합니다. 서버는 BCrypt hash만 저장하고 비밀번호를 로그에 출력하지 않습니다. 생성 후 `APP_BOOTSTRAP_ADMIN_ENABLED`를 `false`로 되돌리고 비밀번호 환경 변수를 제거하십시오. 같은 이메일로 다시 실행하면 계정을 중복 생성하지 않습니다.

선택적 `.env` 파일로 초기 관리자 값을 설정한다면 파일은 UTF-8로 저장해야 하며, 콘솔 코드 페이지에 의존해 작성하거나 변환하지 마십시오. 이미 깨진 표시 이름은 자동 변환하지 않고 사용자 관리 또는 운영자 검토를 거친 파라미터 바인딩 SQL로 수정합니다. 운영 환경에서의 secret 주입과 실패 복구 절차는 [초기 관리자 Bootstrap 운영 문서](docs/operations/admin-bootstrap.md)를 참고하십시오.

## 로그인과 API 인증

서버 시작에는 Base64로 인코딩한 32바이트 이상의 JWT secret이 필요합니다. 저장소 루트에서 실행하면 Spring Boot가 Git에서 제외된 `.env` 파일을 선택적으로 읽습니다. 운영 환경에서는 `.env` 대신 secret manager에서 동일한 값을 환경 변수로 주입해야 하며 저장소나 로그에 기록하면 안 됩니다.

```powershell
$body = @{ email = 'admin@example.com'; password = '<관리자 비밀번호>' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/me -Headers @{ Authorization = "Bearer $($login.accessToken)" }
$refreshBody = @{ refreshToken = $login.refreshToken } | ConvertTo-Json
$rotated = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/refresh -ContentType 'application/json' -Body $refreshBody
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/logout -ContentType 'application/json' -Body (@{ refreshToken = $rotated.refreshToken } | ConvertTo-Json)
```

access token의 기본 만료 시간은 15분, refresh token은 30일입니다. refresh token은 갱신할 때마다 교체되며 이전 token이 재사용되면 같은 로그인 세션의 token이 모두 폐기됩니다. 서버에는 refresh token 원문 대신 SHA-256 hash만 저장됩니다. `/api/v1/auth/login`, `/refresh`, `/logout`, health와 OpenAPI 경로를 제외한 API는 유효한 bearer token이 필요합니다.

비밀번호를 연속 5회 잘못 입력하면 계정은 `LOCKED` 상태가 되며 이후 올바른 비밀번호로도 로그인할 수 없습니다. 임계값은 `APP_LOGIN_MAX_FAILED_ATTEMPTS`로 설정할 수 있고 1 이상이어야 합니다. 성공적으로 로그인하면 누적 실패 횟수가 초기화됩니다. 잠긴 계정의 해제는 사용자 관리 기능에서 별도 권한으로 제공할 예정입니다.

`PUT /api/v1/auth/password`로 현재 비밀번호와 새 비밀번호를 전달해 본인 비밀번호를 변경할 수 있습니다. 새 비밀번호는 초기 관리자와 동일한 강도 정책을 따르고 현재 비밀번호를 재사용할 수 없습니다. 변경에 성공하면 탈취 세션을 차단하기 위해 해당 사용자의 모든 refresh token이 폐기되므로 다시 로그인해야 합니다. 기존 access token은 최대 15분의 잔여 수명 동안 유효할 수 있습니다.

## 검증

```powershell
./gradlew.bat clean test integrationTest architectureTest
Set-Location apps/admin-web
npm run lint
npm run typecheck
npm test
npm run build
docker compose -f ../../infra/compose.yaml config
```

프로젝트 방향과 단계별 완료 기준은 [.docs/PROJECT_PLAN.md](.docs/PROJECT_PLAN.md), 저장소 작업 규칙은 [AGENTS.md](AGENTS.md)를 참고하십시오.

커밋 메시지는 `type: 한글 변경사항` 형식을 사용합니다. Codex의 변경 검증·커밋·PR 절차는 저장소의 `ino-admin-deliver-change` skill이 제공합니다.

브랜치는 `main` 기반의 GitHub Flow로 운영하며 이름과 PR 대상은 CI가 자동 검증합니다.
