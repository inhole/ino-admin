---
name: ino-admin-deliver-change
description: Use when INO Admin의 dev 변경을 main 배치 PR로 전달하거나 Milestone 완료 이슈와 통합 CI를 확인해야 할 때
---

# INO Admin 배치 전달

`main`에는 같은 Milestone의 완료 이슈를 하나의 `dev` → `main` 배치 PR로만 전달한다. 논리 커밋과 `dev`·`main` 동기화는 병합 방식으로 보존한다.

## 순서 계약

1. `dev`에서 working tree가 깨끗한지 확인하고 `git fetch origin main dev`를 실행한다. `git rev-parse dev`와 `git rev-parse origin/dev`가 정확히 같지 않으면 동기화 전까지 중단한다.
2. 정확한 `origin/dev` SHA의 `Dev CI`와, `infra/**` 변경이 있으면 `Dev Infra CI`가 모두 통과했는지 확인한다. 실패·취소·진행 중이거나 다른 SHA의 결과뿐이면 중단한다.
3. 같은 Milestone의 완료 이슈만 수집하고, `origin/main..origin/dev`의 포함 커밋과 각 일반 커밋의 `Refs: #번호`를 대조한다. Milestone 불일치, 누락 이슈, 미완료 이슈가 있으면 중단한다.
4. 하나의 `dev` → `main` PR을 만든다. PR 본문에는 Milestone, 완료 이슈 목록, 포함 커밋 요약, 검증 결과를 적고, 완료 이슈마다 별도 줄의 `Closes #번호`를 넣는다.
5. `Main Integration CI`의 모든 job이 통과할 때까지 기다린다. 하나라도 실패·취소·진행 중이면 병합하지 말고 `dev`에서 수정·push한 뒤 처음부터 확인한다.
6. 승인과 모든 통과 조건을 충족한 뒤 **merge commit**으로만 병합한다. squash merge, rebase merge, `main` 또는 `dev`의 force push는 금지한다.
7. 병합 직후 추가 `dev` 작업 전에 `git fetch origin`, `git switch dev`, `git merge --ff-only origin/main`, `git push origin dev` 순서로 `dev`를 `origin/main`에 fast-forward한다. fast-forward가 실패하면 원인을 해결할 때까지 중단한다.

## 최초 부트스트랩 커밋 예외

정책 도입 전에 작성되어 커밋 본문에 `Refs`를 넣을 수 없었던 아래 커밋만 Issue #40에 연결된 것으로 감사한다.

- `84f5623`: dev 배치 PR 워크플로 설계
- `0323e9d`: dev 배치 PR 구현 계획
- `c11dfad`: Codex worktree 경로 제외
- `1f30a85`: PR 자동 리뷰 이슈 발행 설계
- `4167f81`: PR 자동 리뷰 이슈 발행 설계 통합 전 커밋

최초 `dev → main` PR 본문에 이 SHA와 Issue #40 매핑을 모두 기록한다. 이 예외는 위 SHA에만 적용하며 이후 커밋에는 적용하지 않는다. 다른 `Refs` 누락 커밋이 하나라도 있으면 기존 중단 조건을 그대로 적용한다.

## 사후 감사 보정 예외

공유된 `dev` 이력을 rewrite하지 않기 위해 아래 커밋만 지정 Issue에 연결된 것으로 감사한다.

- `30735752`: RBAC 검증 가이드와 CI 단계 이름 정리 → Issue #55

Issue #55와 이 SHA의 매핑을 해당 `dev → main` 배치 PR 본문에 기록한다. 이 예외는 `30735752` 하나에만 적용하며, 다른 `Refs` 누락 커밋은 기존 중단 조건을 그대로 적용한다.

## Actions 상태별 처리

| 상태 | 처리 |
| --- | --- |
| 모든 `Main Integration CI` job 통과 | merge commit 후 `dev` fast-forward |
| job 실패·취소·진행 중 | 병합 금지, `dev` 수정 또는 완료 대기 |
| Actions 권한·서비스 오류 | 통과로 간주하지 말고 접근 복구·재실행을 요청하며 병합 금지 |

## 중단 조건

- 같은 Milestone이 아닌 이슈, `Closes` 누락, 깨끗하지 않은 working tree, 로컬·원격 `dev` 불일치
- 빠른 CI 또는 `Main Integration CI`의 하나라도 미통과
- squash/rebase 병합이나 force push 요청

PR을 여러 개로 쪼개거나 CI 실패 예외를 만들지 않는다. 이슈가 없거나 포함할 완료 커밋이 없으면 PR·병합을 만들지 말고 사유를 보고한다.
