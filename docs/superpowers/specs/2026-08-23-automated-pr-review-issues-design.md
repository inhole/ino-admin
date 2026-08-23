# 내부 PR 자동 검토 및 GitHub 이슈 발행 설계

## 목적

`main` 브랜치를 대상으로 하는 내부 Pull Request가 생성되거나 갱신될 때 Codex가 변경사항을 검토하고, 수정이 필요한 발견사항을 GitHub 이슈로 자동 발행한다. 자동화는 검토와 발행 권한을 분리하고 동일 문제의 중복 이슈 생성을 방지해야 한다.

## 범위

### 포함

- `main` 대상 내부 PR의 생성, 갱신, 재오픈, draft 해제 시 검토
- PR merge ref 기준의 코드 및 저장소 지침 검토
- 구조화된 발견사항 생성
- P1, P2, P3 발견사항의 건별 GitHub 이슈 발행
- 동일 발견사항의 열린 이슈 재사용과 최신 PR 정보 기록
- 워크플로, 프롬프트, 출력 스키마, 이슈 발행 스크립트의 자동 검증

### 제외

- fork에서 생성된 외부 PR의 자동 검토 및 이슈 발행
- 발견사항이 사라졌을 때 기존 이슈 자동 종료
- PR 코드 자동 수정, 자동 커밋 또는 자동 병합
- GitHub 이슈 외부의 별도 데이터베이스나 상태 저장소
- P1 여부를 근거로 PR 병합을 자동 차단하는 정책

## 트리거와 실행 조건

GitHub Actions의 `pull_request` 이벤트를 사용한다.

- 대상 브랜치: `main`
- 이벤트 유형: `opened`, `synchronize`, `reopened`, `ready_for_review`
- 내부 PR 조건: `github.event.pull_request.head.repo.full_name == github.repository`
- draft 제외: `github.event.pull_request.draft == false`

`synchronize`를 포함하여 최초 생성 이후 추가된 커밋도 재검토한다. 내부 PR 조건은 fork PR이 저장소 secret이나 이슈 쓰기 권한을 사용하는 것을 막는 명시적 방어선이다.

## 아키텍처

워크플로는 검토와 발행을 서로 다른 job으로 분리한다.

### 1. review job

책임은 PR 변경사항을 읽고 구조화된 발견사항을 생성하는 것뿐이다.

- 권한: `contents: read`
- checkout ref: `refs/pull/<number>/merge`
- checkout credential: 저장하지 않음
- 실행 환경: Linux GitHub-hosted runner
- Codex sandbox: `read-only`
- Codex 입력: 저장소에 커밋된 전용 review prompt
- Codex 출력: 저장소에 커밋된 JSON Schema를 만족하는 JSON 파일
- secret: `OPENAI_API_KEY`

프롬프트는 다음 원칙을 강제한다.

- 루트와 변경 파일에 적용되는 `AGENTS.md`를 읽는다.
- `.docs/PROJECT_PLAN.md`의 아키텍처, 보안, 테스트 규칙을 따른다.
- 변경된 코드로 인해 새로 발생한 문제만 보고한다.
- 구체적인 파일과 1-based 라인 번호를 제시한다.
- 재현 가능한 영향이 없는 스타일 선호는 발견사항으로 만들지 않는다.
- 비밀, token, 개인정보 또는 전체 환경 변수 값을 출력하지 않는다.

### 2. publish job

책임은 검토 결과를 검증하고 GitHub 이슈 상태와 동기화하는 것이다.

- 의존성: `review` job 성공
- 권한: `contents: read`, `issues: write`, `pull-requests: read`
- 입력: review job에서 전달한 검토 결과 artifact
- 실행 도구: 저장소에 커밋된 Node.js 스크립트와 GitHub API

Codex 실행 job에는 이슈 쓰기 권한을 주지 않는다. 발행 스크립트는 JSON Schema 검증을 통과한 데이터만 GitHub API에 전달한다.

## 발견사항 출력 계약

최상위 출력은 다음 정보를 포함한다.

- `schemaVersion`: 출력 계약 버전
- `prNumber`: 검토 대상 PR 번호
- `headSha`: 검토한 PR head commit SHA
- `findings`: 발견사항 배열

각 발견사항은 다음 필드를 포함한다.

- `severity`: `P1`, `P2`, `P3` 중 하나
- `title`: 간결한 이슈 제목
- `body`: 영향, 재현 조건, 권장 수정 방향
- `path`: 저장소 상대 파일 경로
- `line`: 1-based 단일 라인 번호

`fingerprint`는 모델 출력 계약에 포함하지 않는다. 발행 스크립트가 검증된 발견사항의 `repository + normalized path + normalized title + severity`를 정규화한 뒤 SHA-256으로 계산한다. commit SHA와 line 번호는 코드 이동이나 새 커밋 때문에 바뀔 수 있으므로 fingerprint 입력에서 제외한다.

## 이슈 발행 정책

발견사항 하나당 이슈 하나를 사용한다.

### 새 발견사항

동일 fingerprint의 열린 이슈가 없으면 새 이슈를 생성한다.

- 제목: `[자동 리뷰][P1|P2|P3] <발견사항 제목>`
- 라벨: `automated-review`, `P1` 또는 `P2` 또는 `P3`
- 본문: 영향, 근거 파일/라인, 원본 PR, 검토 commit, 수정 방향, 검증 기준
- 숨김 marker: `<!-- codex-review-fingerprint:<fingerprint> -->`

필요한 라벨이 없으면 workflow가 정의된 색상과 설명으로 생성한다. 라벨 생성 실패는 이슈 본문 생성과 분리해 처리하며, 라벨 오류 때문에 발견사항 자체를 유실하지 않는다.

### 기존 발견사항

동일 fingerprint의 열린 이슈가 있으면 새 이슈를 만들지 않는다. 대신 기존 이슈에 다음 정보를 담은 댓글을 한 번 추가한다.

- 다시 발견된 PR 번호와 URL
- 최신 head SHA
- 최신 파일과 라인 위치

동일 PR과 동일 head SHA에 대한 댓글이 이미 있으면 아무 작업도 하지 않는다. 이 idempotency key는 숨김 marker로 댓글에 기록한다.

### 해결된 것으로 보이는 발견사항

후속 검토에서 결과에 포함되지 않아도 기존 이슈를 자동 종료하지 않는다. 모델 누락이나 일시적 분석 차이로 이슈가 잘못 닫히는 것을 방지하기 위해 해결 판정은 사람 또는 별도 정책에 맡긴다.

## 오류 처리

- `OPENAI_API_KEY` 누락: review job을 명확한 설정 오류로 실패시킨다.
- Codex 실행 실패: publish job을 실행하지 않는다.
- 출력 파일 누락 또는 JSON 파싱 실패: publish job을 실패시키고 이슈를 생성하지 않는다.
- JSON Schema 불일치: 전체 발행을 중단한다. 일부 데이터만 임의로 발행하지 않는다.
- 발견사항 없음: 성공으로 종료하며 이슈를 만들지 않는다.
- GitHub API 일시 오류: 제한된 지수 backoff로 재시도한다.
- 일부 이슈 발행 실패: 성공한 항목과 실패한 항목을 job summary에 기록하고 job을 실패시킨다. 재실행 시 fingerprint와 idempotency marker로 중복을 막는다.

## 보안 설계

- fork PR은 job 조건에서 제외한다.
- Codex Action은 기본 trusted collaborator 정책을 유지한다.
- PR 제목, 본문, commit message는 명령으로 신뢰하지 않으며 review prompt에서 prompt injection 가능성을 명시한다.
- Codex job은 `contents: read`와 read-only sandbox만 사용한다.
- checkout에서 `persist-credentials: false`를 사용한다.
- `OPENAI_API_KEY`는 GitHub Actions secret으로만 제공하고 로그나 artifact에 기록하지 않는다.
- 이슈 쓰기 권한은 검증된 JSON을 처리하는 publish job에만 부여한다.
- 이슈 본문에 코드 조각 전체나 비밀 가능성이 있는 값을 복사하지 않고 파일·라인 링크와 요약만 포함한다.
- Action은 태그가 아닌 고정 commit SHA로 pin하는 것을 기본으로 하며, 업데이트는 의존성 갱신 PR에서 검토한다.

## 저장소 변경 구성

구현 단계에서는 다음 파일을 추가하거나 변경한다.

- `.github/workflows/codex-pr-review.yml`: 트리거, 권한, review/publish job
- `.github/codex/prompts/pr-review.md`: 저장소 전용 검토 프롬프트
- `.github/codex/schemas/pr-review.schema.json`: 구조화 출력 계약
- `.github/scripts/publish-review-issues.mjs`: 검증, fingerprint, 중복 검색, 이슈 생성/댓글
- `.github/scripts/publish-review-issues.test.mjs`: fingerprint와 발행 정책 단위 테스트
- `README.md`: `OPENAI_API_KEY`와 자동 리뷰 운영 방법

기존 `ci.yml`과 `pr-policy.yml`은 책임을 유지한다. 자동 리뷰 workflow는 기존 빌드·테스트 CI를 대체하지 않는다.

## 검증 전략

### 정적 검증

- workflow YAML 파싱
- JSON Schema 파싱과 fixture 검증
- prompt와 schema 경로 존재 여부 확인
- 최소 권한 및 내부 PR 조건 검사

### 단위 테스트

- 같은 문제에서 동일 fingerprint 생성
- line 또는 commit 변경 시 fingerprint 유지
- 제목 또는 파일 경로가 달라지면 fingerprint 변경
- 신규 발견사항은 이슈 생성
- 기존 열린 fingerprint는 댓글 추가
- 동일 PR/head SHA는 댓글 중복 방지
- 닫힌 이슈는 새 이슈 생성 대상으로 처리
- malformed JSON은 발행 전 실패
- findings 빈 배열은 성공하며 API 호출 없음

### GitHub Actions 검증

- 수동 `workflow_dispatch` dry-run 입력을 제공해 이슈 생성 없이 parsing과 summary를 검증한다.
- 구현 PR에서는 실제 이슈 발행을 막고 dry-run으로 workflow 구조를 검증한다.
- merge 후 최초 내부 테스트 PR에서 end-to-end로 한 건의 fixture 발견사항을 검증한다.

## 운영과 비용

- PR 생성과 갱신마다 Codex API 호출이 발생한다.
- 불필요한 호출을 줄이기 위해 draft와 fork PR을 제외한다.
- 검토 prompt는 변경 diff와 관련 파일에 우선 집중하도록 지시한다.
- workflow summary에 검토 commit, 발견사항 수, 생성·갱신·건너뜀 수를 남긴다.
- API 사용량과 실패율은 GitHub Actions 실행 기록과 OpenAI 프로젝트 사용량에서 점검한다.

## 완료 기준

- 내부 `main` 대상 PR의 지정 이벤트에서만 workflow가 실행된다.
- fork와 draft PR에서는 Codex 및 이슈 발행 job이 실행되지 않는다.
- Codex는 읽기 전용으로 구조화된 발견사항만 출력한다.
- 유효한 신규 발견사항은 건별 이슈로 생성된다.
- 동일 발견사항은 중복 이슈 없이 기존 이슈에 최신 근거가 추가된다.
- 해결 추정만으로 기존 이슈가 자동 종료되지 않는다.
- secret과 쓰기 권한이 최소 범위로 격리된다.
- 스크립트 단위 테스트와 repository verification이 통과한다.
