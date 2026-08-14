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

운영 환경에서의 secret 주입과 실패 복구 절차는 [초기 관리자 Bootstrap 운영 문서](docs/operations/admin-bootstrap.md)를 참고하십시오.

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

커밋 메시지는 `type: 한글 변경사항` 형식을 사용합니다. 세부 규칙과 type 목록은 [Git 커밋 메시지 규칙](docs/development/commit-convention.md)을 참고하십시오.
