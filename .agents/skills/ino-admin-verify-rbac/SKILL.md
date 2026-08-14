---
name: ino-admin-verify-rbac
description: Verify INO Admin role-based access control across the admin web and backend, including role-specific menu visibility, protected-route behavior, HTTP 403 handling, and server-side authorization. Use when changing permissions, roles, menus, protected routes, security configuration, or RBAC-related UI and tests.
---

# Verify INO Admin RBAC

## Preserve the security boundary

- Treat UI visibility as usability only, never as authorization.
- Verify authentication, authorization, and object ownership on the server.
- Keep menu access and API permission checks distinct even when they share permission keys.
- Never expose credentials or tokens in fixtures, logs, screenshots, or reports.

## Verify browser behavior

From `apps/admin-web`:

```shell
npx playwright install chromium
npm run test:e2e
```

Keep the core role scenarios:

- `SUPER_ADMIN`: user, permission, and menu management entries are visible and usable.
- `VIEWER`: management entries are hidden; direct navigation to `/users` surfaces the server's `403` as a permission error.

Browser-network mocks may make role-specific UI scenarios deterministic, but they do not prove backend authorization.

## Verify server enforcement

1. Identify every endpoint and object operation affected by the change.
2. Add or update backend integration tests for an allowed role, a denied role, unauthenticated access, and object ownership when applicable.
3. Verify denied requests use the documented error contract without leaking sensitive details.
4. Run affected module tests and `:apps:admin-server:architectureTest`.
5. Run the relevant integration test task when the changed permission is exercised through HTTP.

## Report results

- Separate browser visibility checks from backend enforcement checks.
- List exact commands and outcomes.
- Report skipped scenarios and environmental limitations explicitly.
- Do not claim RBAC is verified when only menu hiding or mocked browser responses were tested.
