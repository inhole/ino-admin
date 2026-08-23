# dev 배치 PR 및 GitHub Actions 검증 설계

## 1. 배경

현재 개발 흐름은 작은 변경 하나마다 브랜치, 커밋, PR을 만들고 로컬에서 전체 검증을 수행한다. 변경 이력은 세밀하지만 PR 관리와 Codex 검증에 드는 비용이 작업 가치에 비해 크다.

앞으로는 GitHub Issue를 작업 기준으로 삼고, 여러 이슈의 논리적 커밋을 `dev`에 누적한 뒤 하나의 배치 PR로 `main`에 전달한다. 테스트 코드는 변경과 함께 먼저 작성하되, 기본 실행과 최종 판정은 GitHub Actions가 담당한다.

## 2. 목표

- 이슈 하나마다 PR을 만드는 비용을 줄인다.
- 이슈와 커밋의 추적 가능성은 유지한다.
- 작은 작업은 `dev`에 직접 누적하고, 고위험 작업만 feature 브랜치로 격리한다.
- `dev`에서는 빠른 피드백을, `main` 병합 전에는 전체 회귀 검증을 제공한다.
- Codex가 반복해서 같은 절차를 따르도록 저장소 전용 스킬을 제공한다.
- 스킬과 개발 문서는 사용자가 읽기 쉬운 한국어로 작성한다.

## 3. 비목표

- GitHub Actions가 테스트 코드를 자동 생성하게 하지 않는다.
- 모든 이슈에 feature 브랜치나 PR을 강제하지 않는다.
- 별도 release 브랜치나 stacked PR 체계를 도입하지 않는다.
- 이번 변경에서 애플리케이션 기능, API, 데이터베이스 스키마를 수정하지 않는다.

## 4. 작업 단위와 브랜치

### 4.1 작업 단위

| 단위 | 책임 |
|---|---|
| Milestone | 한 번의 `dev → main` 배치에 포함할 이슈 묶음 |
| Issue | 사용자 가치 또는 하나의 검증 가능한 기술 결과 |
| Commit | 테스트, 구현, 문서 등 하나의 논리적 변경 |
| Batch PR | 같은 Milestone의 완료 이슈를 `main`에 전달하는 통합 단위 |

### 4.2 브랜치 역할

- `main`: 항상 배포 가능한 안정 브랜치다. 일반 작업 브랜치에서 직접 PR하지 않고 `dev`의 배치 PR만 받는다.
- `dev`: 완료된 이슈의 커밋을 누적하는 장기 통합 브랜치다.
- feature 브랜치: 고위험, 장기, 병렬 작업을 `dev`에서 임시 격리한다.

feature 브랜치 이름은 사람이 작업하면 `<type>/<issue-number>-<slug>`, Codex가 작업하면 `codex/<issue-number>-<slug>`를 사용한다.

### 4.3 feature 브랜치 선택 기준

다음 중 하나에 해당하면 feature 브랜치를 사용한다.

- Flyway migration 또는 데이터 호환성 변경
- 인증·인가·보안 경계 변경
- 공개 API 또는 공용 설정 변경
- 여러 날에 걸치거나 중간 상태가 실패할 수 있는 작업
- 다른 이슈와 병렬로 진행하는 작업
- 별도 코드 리뷰가 필요한 고위험 변경

그 외 작은 기능, 격리된 버그, 테스트, 문서 변경은 `dev`에 직접 커밋한다.

## 5. GitHub Issue 운영

다음 Issue Form을 제공한다.

1. 기능: 사용자 가치, 범위와 비범위, 완료 조건, 테스트 시나리오, 위험
2. 버그: 재현 절차, 기대 결과, 실제 결과, 영향 범위, 회귀 테스트
3. 기술 작업: 결과물, 변경 범위, 완료 조건, 검증 방법

작업 전에 이슈를 Milestone에 배정한다. 커밋 본문은 `Refs: #123`으로 연결하고 배치 PR 본문은 포함된 각 이슈를 `Closes #123`으로 나열한다.

## 6. 개발 및 병합 흐름

### 6.1 일반 이슈

1. 이슈의 완료 조건과 테스트 시나리오를 확인한다.
2. `dev`를 최신 상태로 갱신한다.
3. 테스트 코드를 구현보다 먼저 작성한다.
4. 구현과 문서를 논리적 커밋으로 나눈다.
5. 각 커밋을 이슈에 연결한다.
6. `dev`에 push하고 빠른 CI 결과를 확인한다.
7. CI가 실패하면 다음 이슈로 넘어가기 전에 같은 이슈에서 수정한다.

### 6.2 고위험 또는 장기 이슈

1. 최신 `dev`에서 feature 브랜치를 생성한다.
2. 테스트 우선으로 작업하고 브랜치에 push한다.
3. `dev` 대상 PR을 열어 빠른 CI와 필요한 리뷰를 받는다.
4. squash하지 않고 merge commit으로 병합해 논리적 커밋을 보존한다.

### 6.3 배치 전달

1. 같은 Milestone의 이슈가 완료되고 `dev` 빠른 CI가 통과했는지 확인한다.
2. `dev → main` PR을 생성하고 포함된 이슈를 모두 닫기 참조로 연결한다.
3. 전체 통합 CI가 통과해야 merge commit 방식으로 병합한다.
4. 병합 후 `dev`를 `main`으로 fast-forward하고 push한다.

Squash 또는 rebase merge는 `dev`와 `main`의 이력을 갈라지게 하므로 배치 PR에 사용하지 않는다.

## 7. GitHub Actions 설계

### 7.1 dev 빠른 CI

실행 조건은 `dev` push와 `dev` 대상 PR이다.

- Backend: `test`, `architectureTest`
- Frontend: `lint`, `typecheck`, `test`
- Infrastructure: 관련 변경이 있을 때 `docker compose -f infra/compose.yaml config`
- 같은 ref에 새 커밋이 오면 이전 실행 취소
- 테스트 결과와 실패 원인을 Actions summary 또는 artifact로 보존

Testcontainers 통합 테스트, Playwright, production build는 빠른 CI에서 제외한다.

### 7.2 main 통합 CI

실행 조건은 `main` 대상 PR과 수동 실행이다. PR 정책은 일반 작업 브랜치가 아닌 `dev`만 `main`을 대상으로 할 수 있게 제한한다.

- Backend: `clean test integrationTest architectureTest`
- PostgreSQL 및 MinIO 서비스 연동
- Frontend: `lint`, `typecheck`, `test`, `build`
- Playwright E2E
- Docker Compose 설정 검증

모든 필수 check가 성공해야 `main`에 병합할 수 있다.

### 7.3 테스트 표현

로컬에서 RED 실행을 생략한 작업은 엄밀한 의미의 로컬 TDD 완료로 보고하지 않는다. 완료 보고에는 다음을 구분한다.

- 테스트를 구현보다 먼저 작성했으면 `test-first 작성`
- GitHub Actions가 통과했으면 `CI 검증 완료`
- 로컬에서 RED-GREEN을 실제 확인했을 때만 `로컬 TDD 수행`

로컬 테스트는 디버깅, 단일 테스트의 빠른 확인, Actions를 사용할 수 없는 경우에 선택적으로 실행한다.

## 8. PR 정책

- `main` 대상 PR의 head는 같은 저장소의 `dev`여야 한다.
- feature 또는 Codex 브랜치의 PR base는 `dev`여야 한다.
- `dev → main` PR에는 하나 이상의 `Closes #번호`가 있어야 한다.
- 배치 PR이 참조하는 이슈는 같은 Milestone에 속해야 한다.
- `dev` 대상 PR은 하나 이상의 `Refs #번호` 또는 `Closes #번호`를 포함해야 한다.
- PR 제목과 커밋 제목은 `type: 한글 변경사항` 형식을 유지한다.
- `main`과 `dev`에 force push하지 않는다.

GitHub 저장소 설정에서 `dev → main` 병합은 merge commit을 사용하고, 필수 check와 최신 base 반영을 요구한다.

### 8.1 최초 정책 부트스트랩

`pull_request_target`은 워크플로 파일이 기본 브랜치에 있어야 실행되므로 새 정책을 추가하는 PR 자체에서는 새 `PR Policy`가 실행되지 않는다. 최초 1회는 유지보수자가 관리하는 같은 저장소 브랜치만 `main`을 대상으로 허용하고, 기본 브랜치의 기존 `PR Policy`와 전체 `CI` 통과 및 사람의 워크플로 보안 리뷰를 병합 조건으로 삼는다. 이 리뷰는 읽기 전용 권한, `github.event.pull_request.base.sha` 체크아웃, PR head 코드 미실행을 확인한다. 새 정책이나 새 `Main Integration CI`가 이 도입 PR에서 통과했다고 주장하지 않는다.

도입 PR을 merge commit으로 병합한 직후 `origin/main`의 정확한 SHA에서 `dev`를 생성한다. 이때부터 부트스트랩 예외는 종료되고 모든 PR에 표준 정책을 적용한다.

## 9. 저장소 스킬

### 9.1 `ino-admin-work-on-issue`

GitHub 이슈를 구현할 때 사용한다.

- 저장소 규칙과 이슈 완료 조건 확인
- `dev` 직접 작업과 feature 브랜치 선택
- 테스트 우선 작성
- 논리적 한국어 커밋과 이슈 참조
- push 후 빠른 CI 확인
- 실패 시 중단, 원인 수정, 재검증

### 9.2 `ino-admin-deliver-change`

Milestone 배치를 `main`에 전달할 때 사용한다.

- 같은 Milestone의 완료 이슈와 커밋 수집
- `dev → main` PR 본문 생성
- 전체 CI 확인
- 실패 수정 및 재실행
- merge commit 병합
- 병합 후 `dev` fast-forward 동기화

두 스킬의 `SKILL.md`와 `agents/openai.yaml`의 사용자 표시 문자열은 한국어로 작성한다. 식별자와 YAML 필드명은 호환성을 위해 영문을 유지한다.

## 10. 실패 및 복구

- `dev` 빠른 CI 실패: 다음 이슈 작업을 중단하고 수정 커밋을 추가한다.
- 잘못된 커밋이 `dev`에 포함됨: force push 대신 revert 커밋으로 복구한다.
- feature PR 실패: feature 브랜치에서 수정하고 `dev` 병합을 보류한다.
- 배치 PR 실패: `dev`에 수정 커밋을 추가해 동일 PR을 갱신한다.
- 배치 병합 후 동기화 실패: `dev`에 새 작업을 push하지 않고 `main`과의 fast-forward 가능 여부부터 복구한다.
- GitHub Actions 사용 불가: 미검증 상태를 명시하고 병합하지 않는다.

## 11. 변경 대상

- 루트 `AGENTS.md`
- `.docs/PROJECT_PLAN.md`의 브랜치, 테스트, Codex 운영 규칙
- `.github/workflows/`의 빠른 CI와 통합 CI
- `.github/ISSUE_TEMPLATE/`의 Issue Form
- `.github/pull_request_template.md`
- `.github/workflows/pr-policy.yml`
- `docs/development/branch-strategy.md`
- `docs/development/commit-convention.md`
- `.agents/skills/ino-admin-work-on-issue/`
- `.agents/skills/ino-admin-deliver-change/`

## 12. 검증 기준

- GitHub Actions YAML이 구문상 유효하다.
- PR 정책 스크립트가 `feature → dev`, `dev → main`, 잘못된 base와 이슈 연결 누락을 구분한다.
- Issue Form YAML이 GitHub 형식에 맞는다.
- 두 스킬이 각각 단독으로 읽혀도 책임과 중단 조건이 명확하다.
- 스킬 검증 도구가 두 스킬을 통과한다.
- 문서, AGENTS 규칙, Actions 명령 사이에 충돌이 없다.
