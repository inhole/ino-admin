---
name: ino-admin-deliver-change
description: Complete and deliver changes in the INO Admin repository with its GitHub Flow, Korean commit convention, required verification, and pull-request policy. Use when preparing, committing, pushing, or opening a PR for changes in this repository, or when deciding the correct branch name, commit title, checks, or PR contents.
---

# Deliver an INO Admin change

## Inspect before changing

1. Read the root `AGENTS.md` and every more-specific `AGENTS.md` that governs files being changed.
2. Read `.docs/PROJECT_PLAN.md` before changing architecture, APIs, persistence, security, or build conventions.
3. Inspect `git status` and preserve unrelated user changes.
4. Keep the change focused on one verifiable objective.

## Use the repository workflow

- Base work on `main` and use short-lived branches.
- Use `<type>/<lowercase-hyphen-slug>` for human branches and `codex/<lowercase-hyphen-slug>` for Codex branches. Keep the full name within 80 characters.
- Use one of `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `style`, `build`, `ci`, `chore`, or `revert` as the type.
- Use stacked PRs only when a genuinely dependent change must proceed before its prerequisite merges. Set the dependent PR base to the prerequisite branch and document the merge order.
- Never bypass branch protection or force-push `main`.

## Verify the change

Select checks from the governing `AGENTS.md` and changed surface. At minimum:

- Backend: run affected module tests and architecture tests.
- Frontend: from `apps/admin-web`, run `npm run lint`, `npm run typecheck`, `npm test`, and `npm run build`.
- Infrastructure: run `docker compose -f infra/compose.yaml config` and relevant health checks.
- Behavior changes: add or update tests.
- Database changes: add a new Flyway migration; never modify an applied migration.

Record every command and outcome for the final response. Treat missing dependencies or unavailable services as warnings only when they are genuine environment limitations.

## Commit

1. Review `git diff`, staged files, and `git status` before committing.
2. Use `type: 한글 변경사항` for the subject.
3. Write the Korean summary as a concise noun phrase without a period. Avoid vague subjects such as `수정` or `여러 가지 작업`.
4. Keep one logical change per commit.
5. Add a Korean body when rationale or operational impact needs explanation.
6. Add `BREAKING CHANGE:` for incompatible API, configuration, or database changes.
7. Add `Refs: #123` or `Closes: #123` when applicable.

Example:

```text
feat: refresh token 재사용 탐지 추가

탈취된 refresh token이 다시 사용되면 같은 token family의 세션을 모두 폐기한다.

Refs: #42
```

## Open the pull request

1. Push the short-lived branch and open a PR against `main` unless it is a documented stacked PR.
2. Use the same `type: 한글 변경사항` format for the PR title.
3. Summarize the focused behavior change, list verification results, and call out migrations, API contracts, configuration changes, security impact, and limitations.
4. Resolve review conversations and required CI checks before squash merge.
5. Delete the remote work branch after merge.

If no files changed, do not create a commit or PR. If files changed and were committed, always complete the requested PR step.
