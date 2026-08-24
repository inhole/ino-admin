# 저장소 작업 지침

## 기준 문서
- 아키텍처, API, 영속성, 보안, 빌드 규칙을 변경하기 전에 `.docs/PROJECT_PLAN.md`를 읽는다.
- 파일을 수정하기 전에 가장 가까운 `AGENTS.md`를 읽는다. 하위 문서는 해당 디렉터리에만 추가 규칙을 적용한다.

## 아키텍처
- 모듈형 모노리스를 유지하고, 재사용 모듈을 추출하기 전에 동작하는 vertical slice를 먼저 구현한다.
- 의존 방향을 지킨다. `apps/admin-server`는 `features/*`와 `modules/common-*`에 의존할 수 있고, feature는 common 모듈에 의존할 수 있다. common 모듈은 app이나 feature에 의존하면 안 된다.
- 순환 의존성과 feature 간 직접 의존성을 피한다. 명시적인 public use case나 event로 통신한다.
- JPA entity를 API에 직접 노출하지 않는다. 애플리케이션 전용 controller, route, DTO를 common 모듈에 두지 않는다.
- public REST endpoint는 `/api/v1`로 시작하고 문서화된 오류·페이지네이션 계약을 유지한다.

## 작업 절차
- 무관한 사용자 변경을 보존하고, 각 변경은 검증 가능한 하나의 목표에만 집중한다.
- 관련 파일과 구간만 조사하는 저맥락 방식을 기본으로 하고, 원시 로그를 과도하게 출력하지 않는다.
- 사용자가 요청하지 않았고 지속적인 필요가 없다면 설계 문서, 구현 계획, ADR, 추가 요약을 만들지 않는다.
- 광범위한 저장소 스캔과 반복되는 전체 테스트보다 집중된 검색과 영향받는 테스트를 우선한다. 단, 필수 테스트·보안 검토·Issue/Milestone 추적·GitHub Actions 검증을 줄이지 않는다.
- 커밋 요약은 `type: 변경사항` 형식의 한글로 작성한다. Issue 구현·커밋·feature/Codex → `dev` PR에는 `ino-admin-work-on-issue`를 사용하고, `ino-admin-deliver-change`는 `dev` → `main` 배치 전달에만 사용한다.
- `dev`를 장기 통합 브랜치로 사용한다. 작고 격리된 변경은 `dev`에 직접 반영하고, 위험하거나 장기간·병렬 작업은 feature 브랜치에서 진행한 뒤 merge commit으로 `dev`에 병합한다.
- `docs/development/branch-strategy.md`의 최초 정책 bootstrap 이후 `main`을 대상으로 하는 PR은 `dev` → `main` 배치 PR만 허용한다. squash·rebase merge를 피하고, 배치 병합 후 `main`에서 `dev`를 fast-forward한다.
- 독립적으로 추적할 가치가 있는 기능, 버그, 기술·운영 작업은 GitHub Issue를 만든다. 작은 단계마다 별도 Issue를 만들기보다 Phase나 vertical slice 단위의 Issue와 checklist를 선호한다.
- 범위 내 문서, 테스트, 리팩토링, 후속 수정은 기준 Issue에 포함한다. 결함이 독립적인 우선순위 관리가 필요하거나 기존 범위 밖이면 별도 버그 Issue를 만든다.
- 생성한 Issue는 구현 전에 Milestone에 할당한다. 일반 커밋은 `Refs: #123`, `dev` → `main` 배치 PR은 같은 Milestone의 완료 Issue를 `Closes #123`으로 연결한다.
- 동작이 변경되면 테스트를 추가하거나 수정한다.
- DB 변경은 모두 Flyway로 관리하고 적용된 migration을 수정하지 않는다. 새 migration을 추가한다.
- 서버 시각은 UTC로 저장하고, 시간 의존적인 도메인 로직에는 `Clock`을 주입한다.
- 추측성 재사용 추상화를 추가하지 않고, 동작과 경계가 검증된 뒤 common 모듈로 추출한다.

## 검증
- 테스트를 먼저 작성한다. GitHub Actions에서 변경 범위에 맞는 검증을 실행한다. `dev`는 `Dev CI`, `infra/**`는 `Dev Infra CI`, `dev` → `main` 배치 PR은 `Main Integration CI`를 적용한다.
- 로컬 테스트는 디버깅이나 집중 검증에 선택적으로 사용한다. 관찰하지 않은 RED-GREEN 주기를 로컬 TDD로 보고하지 않는다.
- 필수 GitHub Actions check가 통과한 뒤에만 완료로 보고한다. Actions를 사용할 수 없으면 미검증 상태로 보고하고 병합하지 않는다.
- 실행한 명령, 결과, 실행하지 못한 check를 보고한다.

## 보안
- secret, 실제 개인정보, 비밀번호, access token, refresh token, private key를 커밋하거나 로그에 남기지 않는다.
- 인증, 인가, 객체 소유권은 서버에서 강제한다. UI 노출 여부는 보안 통제가 아니다.
- HTTP 입력, 파일 업로드, 파일명, 스프레드시트 셀, 렌더링되는 HTML을 신뢰할 수 없는 입력으로 다룬다.
- DB query는 parameter binding을 사용하고, 다운로드에는 안전한 response header를 적용한다.
