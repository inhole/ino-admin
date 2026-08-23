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
- Write commit summaries in Korean using `type: 변경사항`. Use the `ino-admin-deliver-change` skill when preparing commits and pull requests.
- Use `dev` as the long-lived integration branch. Send small, isolated changes directly to `dev`; use a feature branch for risky, long-running, or parallel work and merge it into `dev` with a merge commit.
- Only a `dev` → `main` batch PR may target `main`. Keep logical commits by avoiding squash and rebase merges, and fast-forward `dev` from `main` after the batch merge.
- Track each change with a GitHub Issue and assign it to a Milestone before work begins. A `dev` → `main` batch PR closes only issues from the same Milestone.
- Link ordinary commits with `Refs: #123`; list every completed issue in a batch PR with `Closes #123`.
- Add or update tests whenever behavior changes.
- Manage every database change with Flyway. Never edit an applied migration; add a new migration instead.
- Store server timestamps in UTC and inject `Clock` into time-dependent domain logic.
- Do not add speculative reusable abstractions; extract a common module only after its behavior and boundary are validated.

## Verification
- Write tests first. GitHub Actions runs the verification that matches the changed surface: `Dev CI` for `dev`, `Dev Infra CI` for `infra/**`, and `Main Integration CI` for a `dev` → `main` batch PR.
- Local tests are optional for debugging or a focused single-test check; do not represent an unobserved local RED-GREEN cycle as local TDD.
- Report completion only after the required GitHub Actions checks pass. If Actions is unavailable, report the work as unverified and do not merge it.
- Report commands executed, results, and checks that could not be run.

## Security
- Never commit secrets, real personal data, passwords, access tokens, refresh tokens, or private keys; never log them.
- Enforce authentication, authorization, and object ownership on the server. UI visibility is not a security control.
- Treat HTTP input, file uploads, filenames, spreadsheet cells, and rendered HTML as untrusted input.
- Use parameter binding for database queries and safe response headers for downloads.
