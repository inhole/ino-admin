# 브랜치와 배치 병합 운영

이 저장소는 GitHub Issue를 작업 단위로 사용한다. 완료한 이슈의 논리적 커밋은 장기 통합 브랜치 `dev`에 누적하고, 같은 Milestone의 완료 이슈는 하나의 배치 PR로 `main`에 전달한다.

## 브랜치 역할과 선택

- `main`은 항상 배포 가능한 안정 브랜치이며 `dev → main` 배치 PR만 받는다.
- `dev`는 완료 이슈를 누적하는 장기 통합 브랜치다.
- 작은 기능, 격리된 버그, 테스트, 문서 변경은 `dev`에 직접 커밋한다.
- migration, 보안 경계, 공개 API/공용 설정, 장기·병렬·고위험 작업은 feature 브랜치에서 시작한다. 사람은 `<type>/<issue>-<slug>`, Codex는 `codex/<issue>-<slug>`를 사용한다.

고위험 작업은 **feature branch → dev** PR로 전달한다. `dev` 대상 PR은 빠른 검증과 필요한 리뷰를 마친 뒤 squash 또는 rebase 없이 **merge commit**으로 병합하여 논리적 커밋을 보존한다.

## 이슈부터 커밋까지

작업 전에 Issue의 완료 조건, 테스트 시나리오, 위험을 확인하고 Milestone에 배정한다. 테스트는 구현보다 먼저 작성한다. 일반 이슈는 다음 순서로 `dev`에 누적한다.

```powershell
git switch dev
git pull --ff-only origin dev
git commit -m "feat: 사용자 검색 추가" -m "Refs: #123"
git push origin dev
```

커밋을 push한 뒤 GitHub Actions의 `Dev CI`를 확인한다. `infra/**`를 바꿨다면 `Dev Infra CI`도 확인한다. Actions가 실패하면 다음 이슈로 넘어가지 말고 같은 이슈에서 수정한다.

## 배치 PR과 동기화

Milestone의 이슈가 완료되고 `dev` 빠른 CI가 통과하면 **dev → main** 배치 PR을 만든다. PR 본문에 포함 이슈마다 `Closes #번호`를 적고, `Main Integration CI`의 backend·frontend·E2E·Compose 검증이 모두 통과한 뒤 **merge commit**으로 병합한다. `main`과 `dev`에는 force push하지 않는다.

배치 병합 후 `dev`는 다음 명령으로 `main`에 **fast-forward** 동기화한다.

```powershell
git switch dev
git fetch origin
git merge --ff-only origin/main
git push origin dev
```

동기화가 되지 않으면 새 작업을 push하지 말고 fast-forward 가능 상태부터 복구한다. 잘못 들어간 커밋은 force push 대신 revert 커밋으로 되돌린다. GitHub Actions를 사용할 수 없으면 미검증 상태로 보고하고 병합하지 않는다.
