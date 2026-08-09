# Repository Instructions

## Source of Truth
- Read `.docs/PROJECT_PLAN.md` before changing architecture, APIs, persistence, security, or build conventions.
- Read the nearest `AGENTS.md` before editing; a more specific file may add rules for its directory only.

## Architecture
- Keep the system a modular monolith and implement working vertical slices before extracting reusable modules.
- Respect dependency direction: `apps/admin-server` may depend on `features/*` and `modules/common-*`; features may depend on common modules; common modules must never depend on apps or features.
- Avoid cyclic dependencies and direct feature-to-feature dependencies; communicate through explicit public use cases or events.
- Do not expose JPA entities directly through APIs. Keep application-specific controllers, routes, and DTOs out of common modules.
- Prefix public REST endpoints with `/api/v1` and preserve the documented error and pagination contracts.

## Workflow
- Preserve unrelated user changes and keep each change focused on one verifiable objective.
- Follow `docs/development/commit-convention.md` for commit messages. Write the summary in Korean using `type: 변경사항`.
- Add or update tests whenever behavior changes.
- Manage every database change with Flyway. Never edit an applied migration; add a new migration instead.
- Store server timestamps in UTC and inject `Clock` into time-dependent domain logic.
- Do not add speculative reusable abstractions; extract a common module only after its behavior and boundary are validated.

## Verification
- Backend changes: run affected module tests and architecture tests; for a phase gate run `gradlew clean test integrationTest architectureTest` when those tasks exist.
- Frontend changes: run `npm run lint`, `npm run typecheck`, `npm test`, and `npm run build` from `apps/admin-web`.
- Infrastructure changes: run `docker compose -f infra/compose.yaml config` and relevant health checks.
- Report commands executed, results, and checks that could not be run.

## Security
- Never commit secrets, real personal data, passwords, access tokens, refresh tokens, or private keys; never log them.
- Enforce authentication, authorization, and object ownership on the server. UI visibility is not a security control.
- Treat HTTP input, file uploads, filenames, spreadsheet cells, and rendered HTML as untrusted input.
- Use parameter binding for database queries and safe response headers for downloads.
