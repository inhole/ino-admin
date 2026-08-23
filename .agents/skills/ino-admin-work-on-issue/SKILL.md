---
name: ino-admin-work-on-issue
description: Use when INO Admin의 GitHub 이슈를 구현하거나 dev 직접 작업과 feature 브랜치 중 하나를 선택해야 할 때
---

# INO Admin 이슈 작업

## 작업 순서

1. 작업 전 Issue 번호, Milestone 배정, 완료 조건과 위험을 확인하고 변경 경로의 가장 가까운 `AGENTS.md`를 읽는다. Milestone이 없으면 배정 전에는 구현을 시작하지 않는다.
2. 아래 기준으로 작업 위치를 정한다. `main`은 `dev → main` 배치 PR만 받으므로 일반 이슈나 feature 브랜치의 시작점으로 사용하지 않는다.
3. 테스트를 구현보다 먼저 작성한다.
4. 한 논리적 변경을 `type: 한글 변경사항`으로 커밋하고, 커밋 본문에 반드시 `Refs: #번호`를 넣는다.
5. `dev`에 push한 뒤 `Dev CI`를 확인한다. `infra/**`를 변경했다면 `Dev Infra CI`도 확인한다.
6. 빠른 CI가 실패하거나 Actions를 확인할 수 없으면 다음 Issue로 진행하지 말고 현재 Issue를 수정하거나 미검증 상태로 보고한다.

## 빠른 선택표

| 변경 | 작업 위치 | 전달 |
| --- | --- | --- |
| 작은 기능, 격리된 버그, 테스트, 문서 | `dev` 직접 커밋 | `Dev CI` 통과 확인 |
| migration, 보안 경계, 공개 API·공용 설정, 장기·병렬·고위험 작업 | `feature/Codex` 브랜치 | `dev` 대상 PR을 merge commit으로 병합 |

feature 브랜치는 위 고위험 기준을 하나 이상 충족할 때만 선택하며, 이름은 사람은 `<type>/<issue>-<slug>`, Codex는 `codex/<issue>-<slug>`를 사용한다. feature 브랜치도 `dev`에서 시작해 `dev`로만 전달한다.

## 잘못된 요청의 교정

| 잘못된 관행 | 교정 |
| --- | --- |
| “작은 수정이니 `main` 기반 브랜치” | 작은 격리 수정은 `dev`에 직접 누적한다. |
| “일단 브랜치를 만들자” | 고위험 기준을 확인한 뒤에만 feature 브랜치를 사용한다. |
| “Milestone은 나중에” | Issue의 Milestone과 완료 조건을 먼저 확인한다. |
| 일반 커밋에 `Fixes`·`Closes` | 일반 커밋 본문은 `Refs: #번호`만 사용한다. `Closes`는 `dev → main` 배치 PR용이다. |
| “CI가 실패해도 다음 Issue 진행” | 실패한 현재 Issue에서 중단하고 수정한다. |
