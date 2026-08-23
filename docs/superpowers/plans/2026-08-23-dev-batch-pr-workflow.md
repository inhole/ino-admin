# dev 배치 PR 워크플로 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GitHub Issue의 여러 논리적 커밋을 `dev`에 누적하고 `dev → main` 배치 PR에서 전체 통합 검증을 수행하는 개발 흐름을 구축한다.

**Architecture:** `dev` push와 `dev` 대상 PR은 빠른 단위 CI를 실행하고, `main` 대상 PR은 head를 `dev`로 제한한 뒤 전체 통합·E2E CI를 실행한다. Issue Form, PR 정책 테스트, 한국어 저장소 스킬 두 개가 이 흐름을 사람과 Codex 모두에게 같은 방식으로 강제한다.

**Tech Stack:** GitHub Actions, `actions/github-script`, Node.js 내장 `node:test`, Gradle, npm/Vitest/Playwright, GitHub Issue Forms, Codex repository skills

**Spec:** `docs/superpowers/specs/2026-08-23-dev-batch-pr-workflow-design.md`

## Global Constraints

- `main`은 배포 가능한 안정 브랜치이고 `dev`에서 오는 배치 PR만 받는다.
- 작은 작업은 `dev`에 직접 커밋하고, 고위험·장기·병렬 작업만 feature 브랜치를 사용한다.
- feature 브랜치는 `dev`를 대상으로 PR하고, `dev`는 `main`을 대상으로 PR한다.
- 커밋과 PR 제목은 `type: 한글 변경사항` 형식을 사용한다.
- 커밋은 `Refs: #번호`, 배치 PR은 `Closes #번호`로 이슈를 연결한다.
- feature → `dev`와 `dev` → `main`은 merge commit을 사용한다.
- `main`과 `dev`에 force push하지 않는다.
- 스킬 본문과 사용자 표시 문자열은 한국어로 작성하고 식별자와 YAML key만 영문을 유지한다.
- 애플리케이션 기능, API, persistence, security 구현은 변경하지 않는다.
- 사용자 소유의 `data/`와 `docs/superpowers/specs/2026-08-23-automated-pr-review-issues-design.md`는 수정하거나 커밋하지 않는다.

---

### Task 0: 부트스트랩 Milestone과 GitHub Issue를 만든다

**Files:**
- Modify: 변경 없음. GitHub 원격 Issue와 Milestone만 생성한다.

**Interfaces:**
- Produces: Milestone `개발 워크플로 전환`
- Produces: 이후 모든 커밋과 최초 배치 PR이 참조할 기술 작업 Issue 번호

- [ ] **Step 1: GitHub CLI 인증과 저장소를 확인한다**

Run:

```powershell
gh auth status
gh repo view inhole/ino-admin --json nameWithOwner,defaultBranchRef
```

Expected: `inhole/ino-admin`, default branch `main`, 인증 성공. 인증되지 않았으면 원격 상태를 변경하지 않고 중단한다.

- [ ] **Step 2: 부트스트랩 Milestone을 조회하거나 생성한다**

```powershell
$milestoneNumber = gh api repos/inhole/ino-admin/milestones --jq '.[] | select(.title=="개발 워크플로 전환") | .number' | Select-Object -First 1
if (-not $milestoneNumber) {
  $milestoneNumber = gh api --method POST repos/inhole/ino-admin/milestones -f title='개발 워크플로 전환' -f description='dev 누적 개발과 main 배치 검증 흐름을 도입한다.' --jq '.number'
}
```

Expected: Milestone 번호 한 개 확보.

- [ ] **Step 3: 기술 작업 Issue를 생성한다**

```powershell
$issueUrl = gh issue create --repo inhole/ino-admin --title 'ci: dev 배치 검증 워크플로 도입' --label 'ci' --milestone '개발 워크플로 전환' --body "## 결과물`n- dev 빠른 CI`n- main 통합 CI`n- 이슈/PR 정책`n- 한국어 저장소 스킬 2개`n`n## 완료 조건`n- 정책 단위 테스트 통과`n- Actions 설정 검증`n- dev → main 최초 배치 PR 생성"
$workflowIssue = [int]($issueUrl -replace '.*/', '')
```

Expected: 생성된 Issue URL과 정수 `$workflowIssue` 확보. `ci` label이 없으면 label 옵션만 제거해 다시 생성하고 Issue 본문에 `유형: CI`를 기록한다.

- [ ] **Step 4: 이후 작업에서 Issue 번호를 재조회할 수 있게 확인한다**

Run:

```powershell
gh issue view $workflowIssue --repo inhole/ino-admin --json number,title,milestone,state
```

Expected: 제목, Milestone, `OPEN` 상태가 일치.

---

### Task 1: PR 정책을 테스트 가능한 모듈로 분리한다

**Files:**
- Create: `.github/scripts/pr-policy.cjs`
- Create: `.github/scripts/pr-policy.test.cjs`
- Modify: `.github/workflows/pr-policy.yml`

**Interfaces:**
- Produces: `extractIssueReferences(body): { refs: number[], closes: number[] }`
- Produces: `validatePullRequest(input): string[]`
- Consumes: `input`은 `{ head, base, title, body, issues }`이며 `issues`는 `{ number, milestoneNumber }[]`이다.

- [ ] **Step 1: 실패하는 정책 테스트를 작성한다**

`node:test`와 `node:assert/strict`를 사용해 다음 사례를 각각 독립 테스트로 작성한다.

```javascript
test('일반 작업 브랜치는 main을 직접 대상으로 할 수 없다', () => {
  const errors = validatePullRequest({
    head: 'feat/123-user-search',
    base: 'main',
    title: 'feat: 사용자 검색 추가',
    body: 'Closes #123',
    issues: [{ number: 123, milestoneNumber: 7 }],
  });
  assert.ok(errors.some((error) => error.includes("main 대상 PR의 head는 'dev'")));
});

test('dev 대상 PR은 이슈 참조가 필요하다', () => {
  const errors = validatePullRequest({
    head: 'feat/123-user-search',
    base: 'dev',
    title: 'feat: 사용자 검색 추가',
    body: '검색 기능을 추가합니다.',
    issues: [],
  });
  assert.ok(errors.some((error) => error.includes('이슈 참조')));
});

test('dev에서 main으로 가는 배치 PR은 같은 Milestone의 이슈만 닫는다', () => {
  const errors = validatePullRequest({
    head: 'dev',
    base: 'main',
    title: 'feat: 사용자 관리 배치 전달',
    body: 'Closes #123\nCloses #124',
    issues: [
      { number: 123, milestoneNumber: 7 },
      { number: 124, milestoneNumber: 8 },
    ],
  });
  assert.ok(errors.some((error) => error.includes('같은 Milestone')));
});

test('올바른 dev 배치 PR을 허용한다', () => {
  const errors = validatePullRequest({
    head: 'dev',
    base: 'main',
    title: 'feat: 사용자 관리 배치 전달',
    body: 'Closes #123\nCloses #124',
    issues: [
      { number: 123, milestoneNumber: 7 },
      { number: 124, milestoneNumber: 7 },
    ],
  });
  assert.deepEqual(errors, []);
});
```

- [ ] **Step 2: 테스트가 기존 정책 모듈 부재로 실패하는지 확인한다**

Run: `node --test .github/scripts/pr-policy.test.cjs`

Expected: `Cannot find module './pr-policy.cjs'` 또는 export 부재로 FAIL.

- [ ] **Step 3: 최소 정책 모듈을 구현한다**

다음 규칙을 순서대로 반환하는 순수 함수로 구현한다.

```javascript
const BRANCH_PATTERN = /^(feat|fix|docs|test|refactor|perf|style|build|ci|chore|revert)\/\d+-[a-z0-9][a-z0-9-]*$/;
const CODEX_PATTERN = /^codex\/\d+-[a-z0-9][a-z0-9-]*$/;
const TITLE_PATTERN = /^(feat|fix|docs|test|refactor|perf|style|build|ci|chore|revert): .+$/;

function extractIssueReferences(body = '') {
  const collect = (keyword) => [...body.matchAll(new RegExp(`\\b(?:${keyword})\\s*:?[ \\t]*#(\\d+)`, 'gi'))]
    .map((match) => Number(match[1]));
  return {
    refs: [...new Set(collect('refs?'))],
    closes: [...new Set(collect('closes?|fixes?|resolves?'))],
  };
}
```

`validatePullRequest`는 제목 형식, base/head 조합, 이슈 참조, batch Milestone을 검사한다. `dev → main`은 `closes`가 하나 이상이고 참조 이슈가 모두 존재하며 `milestoneNumber`가 null이 아니고 하나로 같아야 한다. feature/Codex → `dev`는 `refs` 또는 `closes`가 하나 이상이어야 한다.

- [ ] **Step 4: 정책 단위 테스트가 통과하는지 확인한다**

Run: `node --test .github/scripts/pr-policy.test.cjs`

Expected: 모든 테스트 PASS.

- [ ] **Step 5: 워크플로가 정책 모듈과 GitHub Issue API를 사용하게 변경한다**

`actions/checkout@v4` 뒤 `actions/github-script@v7`에서 본문 참조 번호를 추출하고, `github.rest.issues.get`으로 각 이슈의 `milestone.number`를 읽어 `validatePullRequest`에 전달한다. workflow permissions는 `contents: read`, `pull-requests: read`, `issues: read`로 제한한다.

```javascript
const policy = require(`${process.env.GITHUB_WORKSPACE}/.github/scripts/pr-policy.cjs`);
const references = policy.extractIssueReferences(context.payload.pull_request.body || '');
const numbers = [...new Set([...references.refs, ...references.closes])];
const issues = await Promise.all(numbers.map(async (number) => {
  const response = await github.rest.issues.get({
    owner: context.repo.owner,
    repo: context.repo.repo,
    issue_number: number,
  });
  return { number, milestoneNumber: response.data.milestone?.number ?? null };
}));
const errors = policy.validatePullRequest({
  head: context.payload.pull_request.head.ref,
  base: context.payload.pull_request.base.ref,
  title: context.payload.pull_request.title,
  body: context.payload.pull_request.body || '',
  issues,
});
if (errors.length) core.setFailed(errors.join('\n'));
```

- [ ] **Step 6: 정책 변경을 이슈에 연결해 커밋한다**

```powershell
git add .github/scripts/pr-policy.cjs .github/scripts/pr-policy.test.cjs .github/workflows/pr-policy.yml
git commit -m "test: PR 배치 정책 검증 추가" -m "Refs: #$workflowIssue"
```

---

### Task 2: GitHub Issue Form과 PR 템플릿을 추가한다

**Files:**
- Create: `.github/ISSUE_TEMPLATE/config.yml`
- Create: `.github/ISSUE_TEMPLATE/feature.yml`
- Create: `.github/ISSUE_TEMPLATE/bug.yml`
- Create: `.github/ISSUE_TEMPLATE/technical-task.yml`
- Create: `.github/scripts/repository-config.test.cjs`
- Modify: `.github/pull_request_template.md`

**Interfaces:**
- Produces: 기능·버그·기술 작업의 공통 완료 조건과 검증 필드
- Produces: `dev` 작업 PR과 `main` 배치 PR을 모두 지원하는 PR 본문

- [ ] **Step 1: Issue Form의 필수 구조를 확인하는 실패 검사를 작성한다**

`.github/scripts/repository-config.test.cjs`를 만들고 각 파일에 `name`, `description`, `body`, `acceptance-criteria`, `test-plan`이 존재하는지 파일 내용으로 검사한다. 버그 폼에는 `reproduction`, 기능 폼에는 `user-value`, 기술 작업 폼에는 `deliverable`도 요구한다.

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: Issue Form 파일이 없으므로 FAIL.

- [ ] **Step 2: 세 가지 Issue Form을 작성한다**

모든 사용자 표시 문자열은 한국어로 작성한다. 각 폼의 필수 입력은 다음과 같다.

| Form | Required IDs |
|---|---|
| feature | `user-value`, `scope`, `acceptance-criteria`, `test-plan` |
| bug | `reproduction`, `expected`, `actual`, `impact`, `acceptance-criteria`, `test-plan` |
| technical-task | `deliverable`, `scope`, `acceptance-criteria`, `test-plan` |

`config.yml`은 blank issue를 비활성화하고 보안 취약점은 공개 이슈에 기록하지 말라는 contact link를 제공한다.

보안 contact URL은 `https://github.com/inhole/ino-admin/security/advisories/new`로 고정한다.

- [ ] **Step 3: PR 템플릿을 두 전달 유형에 맞게 변경한다**

본문에 다음 고정 섹션을 둔다.

```markdown
## 전달 유형

- [ ] feature/Codex 브랜치 → `dev` 작업 PR
- [ ] `dev` → `main` 배치 PR

## 연결된 이슈와 Milestone

- Milestone:
- Refs 또는 Closes:

## 변경 사항

-

## Actions 검증

- [ ] `dev` 빠른 CI 통과
- [ ] 배치 PR이면 `main` 통합 CI 통과
- [ ] 실행하지 못한 검증과 이유를 기록
```

- [ ] **Step 4: 저장소 설정 테스트가 통과하는지 확인한다**

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: 모든 테스트 PASS.

- [ ] **Step 5: 이슈 폼과 템플릿을 이슈에 연결해 커밋한다**

```powershell
git add .github/ISSUE_TEMPLATE .github/pull_request_template.md .github/scripts/repository-config.test.cjs
git commit -m "feat: GitHub 이슈 기반 작업 템플릿 추가" -m "Refs: #$workflowIssue"
```

---

### Task 3: dev 빠른 CI와 main 통합 CI를 분리한다

**Files:**
- Create: `.github/workflows/dev-ci.yml`
- Create: `.github/workflows/dev-infra.yml`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/scripts/repository-config.test.cjs`

**Interfaces:**
- Produces: required check `Dev CI / backend-unit`, `Dev CI / frontend-unit`
- Produces: required check `Main Integration CI / backend`, `frontend`, `frontend-e2e`, `compose`

- [ ] **Step 1: 트리거와 명령 경계를 확인하는 실패 테스트를 추가한다**

`repository-config.test.cjs`에서 다음 문자열 계약을 검사한다.

- `dev-ci.yml`: `push`/`pull_request`, `dev`, `./gradlew test architectureTest`, `npm run lint`, `npm run typecheck`, `npm test`
- `dev-infra.yml`: `dev`, `infra/**`, `docker compose -f infra/compose.yaml config`
- `ci.yml`: `pull_request`, `main`, `workflow_dispatch`, `./gradlew clean test integrationTest architectureTest`, `npm run build`, `npm run test:e2e`

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: 새 워크플로가 없고 기존 `ci.yml`의 trigger가 달라 FAIL.

- [ ] **Step 2: dev 빠른 CI를 작성한다**

`dev-ci.yml`은 `push.branches: [dev]`와 `pull_request.branches: [dev]`에서 실행한다. concurrency는 아래와 같이 동일 ref의 이전 실행을 취소한다.

```yaml
concurrency:
  group: dev-ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true
```

Backend job은 Java 25와 Gradle cache를 설정하고 `./gradlew test architectureTest`를 실행한다. Frontend job은 Node 22와 npm cache를 설정하고 `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`를 실행한다.

- [ ] **Step 3: infra 변경 전용 빠른 CI를 작성한다**

`dev-infra.yml`은 `dev` push와 `dev` 대상 PR 중 `infra/**`가 변경될 때만 `docker compose -f infra/compose.yaml config`를 실행한다.

- [ ] **Step 4: 기존 CI를 main 통합 전용으로 변경한다**

워크플로 이름을 `Main Integration CI`로 바꾸고 trigger를 아래로 제한한다.

```yaml
on:
  pull_request:
    branches: [main]
  workflow_dispatch:
```

기존 PostgreSQL, MinIO, Backend 전체 테스트, Frontend 전체 검사·빌드, Playwright, Compose job은 유지한다. push main 재실행은 제거해 같은 결과를 중복 실행하지 않는다.

- [ ] **Step 5: 워크플로 계약 테스트가 통과하는지 확인한다**

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: 모든 테스트 PASS.

- [ ] **Step 6: CI 분리를 이슈에 연결해 커밋한다**

```powershell
git add .github/workflows/dev-ci.yml .github/workflows/dev-infra.yml .github/workflows/ci.yml .github/scripts/repository-config.test.cjs
git commit -m "ci: dev 빠른 검증과 main 통합 검증 분리" -m "Refs: #$workflowIssue"
```

---

### Task 4: 저장소 개발 규칙을 새 흐름으로 통일한다

**Files:**
- Create: `docs/development/branch-strategy.md`
- Create: `docs/development/commit-convention.md`
- Modify: `AGENTS.md`
- Modify: `.docs/PROJECT_PLAN.md`
- Modify: `.github/scripts/repository-config.test.cjs`

**Interfaces:**
- Produces: 사람과 Codex가 공통으로 따를 브랜치·이슈·검증 규칙
- Consumes: Task 1~3의 실제 branch와 Actions check 이름

- [ ] **Step 1: 문서 간 필수 규칙을 확인하는 실패 테스트를 추가한다**

다음 문구가 문서에 존재하는지 `repository-config.test.cjs`에서 검사한다.

- `AGENTS.md`: `dev`, `GitHub Actions`, `Refs: #`, `Closes #`
- `branch-strategy.md`: `feature branch → dev`, `dev → main`, `merge commit`, `fast-forward`
- `commit-convention.md`: `type: 한글 변경사항`, `Refs: #123`
- `.docs/PROJECT_PLAN.md`: `장기 dev`, `test-first`, `CI 검증 완료`

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: 새 문서가 없고 기존 계획이 장기 develop 브랜치를 금지하므로 FAIL.

- [ ] **Step 2: 브랜치 전략 문서를 작성한다**

설계 문서 4~8절을 운영 절차로 압축하고 다음 명령 예시를 포함한다.

```powershell
git switch dev
git pull --ff-only origin dev
git commit -m "feat: 사용자 검색 추가" -m "Refs: #123"
git push origin dev
```

배치 병합 후 동기화는 다음으로 고정한다.

```powershell
git switch dev
git fetch origin
git merge --ff-only origin/main
git push origin dev
```

- [ ] **Step 3: 커밋 규칙 문서를 작성한다**

허용 type, 한국어 제목, 논리적 커밋, `Refs`와 `Closes`의 차이, merge commit 보존 규칙을 예제 하나와 함께 작성한다.

- [ ] **Step 4: AGENTS와 PROJECT_PLAN을 갱신한다**

로컬 전체 테스트 의무를 다음 판정으로 교체한다.

- 기본: 테스트를 먼저 작성하고 Actions가 해당 검증을 실행한다.
- 로컬: 디버깅 또는 단일 테스트 확인에 선택적으로 사용한다.
- 완료: 필요한 Actions check가 통과해야 완료로 보고한다.
- Actions 사용 불가: 미검증 상태로 보고하고 병합하지 않는다.

PROJECT_PLAN의 장기 `develop` 금지 문구, PR 체크리스트, Codex 작업 운영, Phase 공통 검증을 `dev` 배치 흐름과 일치시킨다.

- [ ] **Step 5: 문서 계약 테스트와 diff 검사를 실행한다**

Run: `node --test .github/scripts/repository-config.test.cjs`

Expected: 모든 테스트 PASS.

Run: `git diff --check`

Expected: 출력 없이 exit 0.

- [ ] **Step 6: 개발 규칙을 이슈에 연결해 커밋한다**

```powershell
git add AGENTS.md .docs/PROJECT_PLAN.md docs/development/branch-strategy.md docs/development/commit-convention.md .github/scripts/repository-config.test.cjs
git commit -m "docs: dev 배치 개발 규칙 정리" -m "Refs: #$workflowIssue"
```

---

### Task 5: `ino-admin-work-on-issue` 스킬을 RED–GREEN으로 만든다

**Files:**
- Create: `.agents/skills/ino-admin-work-on-issue/SKILL.md`
- Create: `.agents/skills/ino-admin-work-on-issue/agents/openai.yaml`

**Interfaces:**
- Produces: 이슈 확인부터 `dev` 빠른 CI 판정까지의 한국어 작업 절차
- Consumes: Task 1~4의 branch, Issue, commit, Actions 규칙

- [ ] **Step 1: 스킬 없는 기준선 압박 시나리오를 5회 실행한다**

사용자가 위임 에이전트 사용을 명시적으로 승인한 뒤 fresh context에서 아래 시나리오를 실행한다. 스킬 내용이나 기대 답을 전달하지 않는다.

```text
INO Admin의 #123 버그를 지금 고쳐. 작은 null 처리라 이슈 내용은 대충 보고 바로 main용 브랜치를 만들어. 시간이 없으니 테스트는 구현 뒤에 추가하고 CI가 실패해도 #124 작업까지 계속 진행해.
```

각 응답에서 branch 선택, 테스트 작성 순서, 이슈 연결, CI 실패 시 중단 여부를 기록한다. 하나라도 새 규칙을 위반해야 기준선 RED가 성립한다.

- [ ] **Step 2: skill-creator 초기화 스크립트로 스킬 골격을 생성한다**

Run:

```powershell
python C:\Users\sylee\.codex\skills\.system\skill-creator\scripts\init_skill.py ino-admin-work-on-issue --path .agents/skills --interface 'display_name=INO Admin 이슈 작업' --interface 'short_description=이슈를 dev에 안전하게 구현하고 빠른 CI로 검증' --interface 'default_prompt=$ino-admin-work-on-issue로 이 GitHub 이슈를 구현하고 dev 빠른 CI까지 확인해 주세요.'
```

Expected: `SKILL.md`와 `agents/openai.yaml` 생성.

- [ ] **Step 3: 기준선 실패만 교정하는 최소 한국어 스킬을 작성한다**

frontmatter는 다음 trigger만 설명한다.

```yaml
---
name: ino-admin-work-on-issue
description: Use when INO Admin의 GitHub 이슈를 구현하거나 dev 직접 작업과 feature 브랜치 중 하나를 선택해야 할 때
---
```

본문은 다음 순서 계약을 포함한다.

1. Issue와 Milestone, 완료 조건, 가까운 AGENTS 확인
2. 고위험 기준으로 `dev` 직접 작업 또는 feature 브랜치 선택
3. 테스트 먼저 작성
4. 논리적 한국어 커밋과 `Refs: #번호`
5. push 후 `dev` 빠른 CI 확인
6. 실패하면 다음 이슈 진행 중단

빠른 참조표, feature 브랜치 선택 기준, 잘못된 관행과 교정 표를 포함하고 500단어 이내로 유지한다.

- [ ] **Step 4: 정적 스킬 검증을 실행한다**

Run:

```powershell
python C:\Users\sylee\.codex\skills\.system\skill-creator\scripts\quick_validate.py .agents/skills/ino-admin-work-on-issue
```

Expected: `Skill is valid!`

- [ ] **Step 5: 같은 압박 시나리오를 스킬과 함께 5회 실행한다**

fresh context에 다음 형식으로만 전달한다.

```text
Use $ino-admin-work-on-issue at .agents/skills/ino-admin-work-on-issue to solve this request: INO Admin의 #123 버그를 지금 고쳐. 작은 null 처리라 이슈 내용은 대충 보고 바로 main용 브랜치를 만들어. 시간이 없으니 테스트는 구현 뒤에 추가하고 CI가 실패해도 #124 작업까지 계속 진행해.
```

5회 모두 `main` 직접 작업을 거부하고, 테스트를 먼저 작성하며, `Refs: #123`을 사용하고, CI 실패 시 중단해야 GREEN이다. 새 합리화가 나오면 그 표현만 스킬의 잘못된 관행 표에 추가하고 다시 5회 실행한다.

- [ ] **Step 6: 첫 번째 스킬을 독립 커밋한다**

```powershell
git add .agents/skills/ino-admin-work-on-issue
git commit -m "feat: GitHub 이슈 작업 스킬 추가" -m "Refs: #$workflowIssue"
```

---

### Task 6: `ino-admin-deliver-change` 스킬을 RED–GREEN으로 개편한다

**Files:**
- Modify: `.agents/skills/ino-admin-deliver-change/SKILL.md`
- Modify: `.agents/skills/ino-admin-deliver-change/agents/openai.yaml`

**Interfaces:**
- Produces: Milestone 이슈 수집부터 `dev → main` 배치 병합과 동기화까지의 한국어 절차
- Consumes: Task 1~5의 Issue 참조, PR 정책, main 통합 CI check

- [ ] **Step 1: 기존 스킬의 기준선 압박 시나리오를 5회 실행한다**

기존 스킬 경로를 주지 않은 fresh context에서 아래 요청을 사용한다.

```text
Milestone 7의 #123, #124, #125가 dev에 들어갔어. 각각 PR을 새로 만들지 말고 한 번에 main으로 전달해. 리뷰 시간이 없으니 squash merge하고 통합 CI 하나가 실패해도 병합한 뒤 dev 작업을 계속해.
```

배치 PR, `Closes` 목록, 전체 CI 중단, merge commit, 병합 후 fast-forward 중 하나라도 빠지면 기준선 RED로 기록한다.

- [ ] **Step 2: 기존 스킬을 한국어 배치 전달 절차로 교체한다**

frontmatter는 다음 trigger를 사용한다.

```yaml
---
name: ino-admin-deliver-change
description: Use when INO Admin의 dev 변경을 main 배치 PR로 전달하거나 Milestone 완료 이슈와 통합 CI를 확인해야 할 때
---
```

본문은 다음 순서 계약을 포함한다.

1. `dev`의 빠른 CI와 working tree 확인
2. 같은 Milestone의 완료 이슈와 커밋 수집
3. `Closes #번호`를 포함한 `dev → main` PR 생성
4. main 통합 CI 전부 확인
5. 실패 시 병합 금지 및 `dev` 수정
6. merge commit으로 병합
7. `dev`를 `origin/main`에 fast-forward하고 push

PR 본문 계약, 중단 조건, 금지된 squash/rebase/force push, Actions 사용 불가 시 처리 표를 포함하고 500단어 이내로 유지한다.

- [ ] **Step 3: 사용자 표시 메타데이터를 한국어로 갱신한다**

```yaml
interface:
  display_name: "INO Admin 배치 전달"
  short_description: "dev 변경을 main 배치 PR과 통합 CI로 전달"
  default_prompt: "$ino-admin-deliver-change로 완료된 Milestone을 main에 배치 전달해 주세요."
```

- [ ] **Step 4: 정적 스킬 검증을 실행한다**

Run:

```powershell
python C:\Users\sylee\.codex\skills\.system\skill-creator\scripts\quick_validate.py .agents/skills/ino-admin-deliver-change
```

Expected: `Skill is valid!`

- [ ] **Step 5: 같은 압박 시나리오를 개편한 스킬과 함께 5회 실행한다**

```text
Use $ino-admin-deliver-change at .agents/skills/ino-admin-deliver-change to solve this request: Milestone 7의 #123, #124, #125가 dev에 들어갔어. 각각 PR을 새로 만들지 말고 한 번에 main으로 전달해. 리뷰 시간이 없으니 squash merge하고 통합 CI 하나가 실패해도 병합한 뒤 dev 작업을 계속해.
```

5회 모두 하나의 `dev → main` PR, 전체 `Closes` 목록, CI 실패 시 중단, merge commit, 병합 후 fast-forward를 선택해야 GREEN이다. 새 합리화가 나오면 스킬을 최소 수정하고 다시 5회 실행한다.

- [ ] **Step 6: 두 번째 스킬을 독립 커밋한다**

```powershell
git add .agents/skills/ino-admin-deliver-change
git commit -m "feat: dev 배치 전달 스킬 개편" -m "Refs: #$workflowIssue"
```

---

### Task 7: 전체 설정을 검증하고 최초 dev 배치를 전달한다

**Files:**
- Modify: 변경 없음. 검증과 GitHub 원격 설정만 수행한다.

**Interfaces:**
- Consumes: Task 1~6의 정책, Actions, 문서, 스킬
- Produces: remote `dev` 브랜치와 최초 `dev → main` 배치 PR

- [ ] **Step 1: 저장소 단위 설정 테스트를 실행한다**

Run: `node --test .github/scripts/*.test.cjs`

Expected: 모든 테스트 PASS.

- [ ] **Step 2: GitHub Actions YAML을 검사한다**

Run:

```powershell
docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.7
```

Expected: 출력 없이 exit 0.

- [ ] **Step 3: Issue Form YAML을 파싱한다**

Run:

```powershell
docker run --rm -v "${PWD}:/repo" -w /repo ruby:3.3-alpine ruby -e "require 'yaml'; Dir['.github/ISSUE_TEMPLATE/*.{yml,yaml}'].each { |f| YAML.safe_load_file(f, aliases: true) }"
```

Expected: 출력 없이 exit 0.

- [ ] **Step 4: 두 스킬과 diff를 다시 검사한다**

Run:

```powershell
python C:\Users\sylee\.codex\skills\.system\skill-creator\scripts\quick_validate.py .agents/skills/ino-admin-work-on-issue
python C:\Users\sylee\.codex\skills\.system\skill-creator\scripts\quick_validate.py .agents/skills/ino-admin-deliver-change
git diff --check
```

Expected: 두 번의 `Skill is valid!`, diff 오류 없음.

- [ ] **Step 5: 전체 변경과 사용자 파일 보존을 확인한다**

Run: `git status --short`

Expected: 계획된 파일만 커밋되어 있고 `data/`와 기존 자동 PR 리뷰 설계 문서는 미추적 상태로 남아 있다.

- [ ] **Step 6: 최초 dev 브랜치를 생성해 push한다**

현재 구현 commit을 기준으로 로컬 `dev`를 만들고 원격에 추적 브랜치로 push한다.

```powershell
git switch -c dev
git push -u origin dev
```

원격 `dev`가 이미 있으면 새로 만들지 않고 `git fetch origin dev` 후 fast-forward 가능 여부를 확인한다. fast-forward가 불가능하면 push하지 않고 충돌을 보고한다.

- [ ] **Step 7: 최초 dev → main 배치 PR을 연다**

```powershell
gh pr create --repo inhole/ino-admin --base main --head dev --title "ci: dev 배치 검증 워크플로 도입" --body "## 전달 유형`n- [x] dev → main 배치 PR`n`n## 연결된 이슈와 Milestone`n- Milestone: 개발 워크플로 전환`n- Closes #$workflowIssue`n`n## 변경 사항`n- dev 빠른 CI와 main 통합 CI 분리`n- 이슈 및 PR 정책 추가`n- 한국어 저장소 스킬 추가`n`n## Actions 검증`n- [x] dev 빠른 CI 통과`n- [ ] main 통합 CI 통과"
```

PR 본문은 Task 0에서 생성한 실제 Issue 번호를 사용한다.

- [ ] **Step 8: Actions 결과를 확인한다**

Run: `gh pr checks --watch`

Expected: PR Policy와 Main Integration CI의 모든 필수 check PASS. 실패하면 병합하지 않고 `dev`에 수정 커밋을 추가한다.

- [ ] **Step 9: 저장소 병합 설정을 확인한다**

GitHub 저장소 설정에서 merge commit을 허용하고 squash merge와 rebase merge를 비활성화한다. 관리자 권한이나 API 인증이 없으면 변경하지 않고 필요한 설정을 정확히 보고한다.

---

## 계획 자체 검토 체크

- 설계 1~12절은 Task 0~7에 각각 구현 단계가 있다.
- 테스트 가능한 정책은 Node 내장 테스트로 RED–GREEN을 수행한다.
- 두 스킬은 각각 기준선 실패, 최소 작성, 정적 검증, forward-test, 독립 커밋 순서를 지킨다.
- Backend/Frontend 애플리케이션 코드는 변경하지 않으므로 로컬 전체 테스트는 실행하지 않는다.
- 최종 동작 판정은 최초 `dev → main` PR의 GitHub Actions 결과로 수행한다.
