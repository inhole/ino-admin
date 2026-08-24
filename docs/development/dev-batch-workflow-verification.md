# dev 배치 워크플로 전환 검증 기록

## 연결 정보

- GitHub Issue: [#40](https://github.com/inhole/ino-admin/issues/40)
- Milestone: `개발 워크플로 전환`
- 원격 `dev` 최초 검증 SHA: `83494446574a31d9f3a1492ec192e3ba379891e0`
- Dev CI: [실행 #32626795128](https://github.com/inhole/ino-admin/actions/runs/32626795128)

## 적용 결과

- 장기 통합 브랜치 `dev`와 선택적 Issue 번호 기반 feature/Codex 브랜치 전략을 문서화했다.
- 일반 변경은 `Refs: #번호`, `dev → main` 배치 PR은 같은 Milestone의 `Closes #번호`를 사용한다.
- `Dev CI`는 backend 단위·아키텍처 테스트와 frontend lint·typecheck·단위 테스트를 실행한다.
- `Main Integration CI`는 `main` 대상 PR에서 backend 통합 테스트, frontend build·E2E, Compose 검증까지 실행한다.
- PR 정책은 신뢰된 base SHA의 정책 코드를 사용하고 fork 저장소의 `dev → main` 요청을 거부한다.
- Issue 작업과 배치 전달 절차를 한국어 저장소 스킬로 제공한다.

## 확인 결과

- Node 정책·설정 계약 테스트: `19/19` 통과
- Actionlint 1.7.7: 통과
- `ino-admin-work-on-issue`, `ino-admin-deliver-change` 정적 검증: 통과
- 원격 `Dev CI`: `frontend-unit`, `backend-unit` 모두 통과
- 기존 사용자 데이터 파일은 커밋 대상에서 제외했다.

## 남은 원격 검증

- 최초 `dev → main` 부트스트랩 PR에서 `Main Integration CI` 전체 job을 확인한다.
- 도입 PR에는 자동 `PR Policy` check가 없으므로 기존 PR 정책을 사람이 직접 대조하고 워크플로 신뢰 경계를 보안 리뷰한다.
- 저장소가 merge commit만 허용하고 squash/rebase merge를 비활성화했는지 확인한다.
- 부트스트랩 병합 후 `dev`를 `origin/main`으로 fast-forward한다.

## 최초 부트스트랩 커밋 예외

정책 도입 전에 생성된 다음 커밋은 최초 PR 본문에서만 Issue #40에 매핑한다.

- `84f5623`: dev 배치 PR 워크플로 설계
- `0323e9d`: dev 배치 PR 구현 계획
- `c11dfad`: Codex worktree 경로 제외
- `1f30a85`: PR 자동 리뷰 이슈 발행 설계
- `4167f81`: PR 자동 리뷰 이슈 발행 설계 통합 전 커밋

PR 본문에는 위 SHA를 모두 기록한다. 이 예외는 고정된 다섯 SHA에만 적용하며 이후 커밋에는 적용하지 않는다. 다른 `Refs` 누락은 배치 전달 중단 조건이다.

## 사후 감사 보정

- `30735752`: RBAC 검증 가이드와 CI 단계 이름 정리 → Issue #55

이미 공유된 `dev` 이력을 rewrite하지 않기 위해 위 SHA만 Issue #55에 연결된 것으로 감사한다. 해당 배치 PR 본문에 이 매핑을 기록하며, 다른 `Refs` 누락은 계속 배치 전달 중단 조건이다.

## 로컬 상태 주의

원격 `Dev CI`는 `8349444`를 검증했다. 이후 로컬 `dev`에는 별도 작업의 merge commit `a1a4115`가 추가되었으므로, 이를 원격에 전달할 때는 해당 SHA에 대한 새 `Dev CI` 결과를 별도로 확인해야 한다.
