# Git 브랜치 전략

이 저장소는 `main` 하나만 장기 유지하는 GitHub Flow를 사용한다. 별도의 `develop` 브랜치를 두지 않으며, 모든 변경은 짧게 유지되는 작업 브랜치와 Pull Request를 통해 `main`에 반영한다.

## 브랜치 구성

### `main`

- 항상 빌드와 배포가 가능한 상태를 유지한다.
- 직접 push와 force push를 금지한다.
- 필수 CI와 리뷰가 통과한 Pull Request만 squash merge한다.
- 릴리스는 `main`의 검증된 커밋에 태그를 생성한다.

### 작업 브랜치

사람이 만드는 브랜치는 `<type>/<slug>`, Codex가 만드는 브랜치는 `codex/<slug>` 형식을 사용한다.

```text
feat/auth-login
fix/file-path-validation
docs/branch-strategy
codex/refresh-token-rotation
```

`type`은 `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `style`, `build`, `ci`, `chore`, `revert` 중 하나를 사용한다. `slug`는 소문자 영문, 숫자, 하이픈만 사용하고 브랜치 전체 길이는 80자를 넘지 않는다. Dependabot의 `dependabot/*` 브랜치는 예외로 허용한다.

작업 브랜치는 하나의 검증 가능한 목적만 포함하고 PR이 merge되면 삭제한다. 장기간 유지되는 기능 통합 브랜치는 만들지 않는다.

## 작업 흐름

1. 최신 `main`에서 작업 브랜치를 만든다.
2. 저장소의 커밋 규칙에 따라 논리적 변경 단위로 커밋한다.
3. 원격 브랜치에 push하고 초기에 Draft PR을 연다.
4. 관련 테스트와 문서를 갱신하고 CI를 통과시킨다.
5. 리뷰 대화를 해결한 뒤 Ready for review로 전환한다.
6. 승인 후 squash merge하고 원격 작업 브랜치를 삭제한다.

PR 제목은 커밋 제목과 동일한 `type: 한글 변경사항` 형식을 사용한다. DB migration, API 계약, 설정 변경은 관련 구현과 같은 PR에 포함한다.

## Stacked PR

선행 변경이 아직 merge되지 않았지만 후속 작업을 분리해야 할 때만 stacked PR을 허용한다.

1. 후속 브랜치를 선행 브랜치에서 만든다.
2. 후속 PR의 base를 선행 브랜치로 지정하고 `stacked` 라벨을 붙인다.
3. PR 설명에 선행 PR 링크와 merge 순서를 기록한다.
4. 선행 PR이 merge되면 후속 PR의 base를 즉시 `main`으로 변경하고 최신 `main`을 반영한다.
5. `stacked` 라벨을 제거하고 일반 PR 검증을 다시 통과시킨다.

서로 독립적인 변경을 편의상 stacked PR로 만들거나 여러 작업자가 공유하는 장기 브랜치로 사용하지 않는다.

## 긴급 수정과 되돌리기

- 운영 긴급 수정도 최신 `main`에서 `fix/<slug>` 브랜치를 만들어 동일한 PR 검증을 거친다.
- 장애 완화를 위해 기존 변경을 되돌려야 하면 `revert/<slug>`를 사용한다.
- 긴급 상황에서도 branch protection을 우회하지 않는다. 필요한 경우 리뷰 및 배포 승인을 신속하게 진행한다.

## GitHub 저장소 보호 설정

`main` ruleset에는 다음 설정을 적용한다.

- Pull Request 필수, 최소 승인 1명
- 새 커밋 push 시 오래된 승인 취소
- 모든 review conversation 해결 필수
- `PR Policy / validate`, `CI / backend`, `CI / frontend`, `CI / compose` status check 필수
- force push 및 branch deletion 금지
- merge 방식은 squash merge만 허용

저장소의 [PR 정책 workflow](../../.github/workflows/pr-policy.yml)는 브랜치명, PR 제목과 base 브랜치를 자동 검증한다. GitHub ruleset은 이 workflow와 기존 CI job을 required check로 지정하여 우회되지 않게 한다.
