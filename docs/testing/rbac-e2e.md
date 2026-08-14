# RBAC E2E 검증

관리자 웹의 역할별 메뉴와 서버 권한 거부 처리를 Playwright로 검증한다.

- `SUPER_ADMIN`: 사용자, 권한, 메뉴 관리 메뉴가 모두 표시된다.
- `VIEWER`: 관리 메뉴가 표시되지 않으며 `/users` 직접 접근 시 서버의 `403` 응답을 권한 오류로 표시한다.

로컬 실행은 `apps/admin-web`에서 다음 명령을 사용한다.

```shell
npx playwright install chromium
npm run test:e2e
```

GitHub Actions의 `frontend-e2e` 작업은 Chromium을 설치하고 위 시나리오를 독립적으로 실행한다. API 응답은 브라우저 네트워크 계층에서 역할별로 고정하며, 실제 백엔드 권한 집행은 백엔드 통합 테스트가 검증한다.
