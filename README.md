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
