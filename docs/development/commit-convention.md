# 커밋 규칙

커밋과 PR 제목은 `type: 한글 변경사항` 형식을 사용한다. 제목은 마침표 없는 간결한 한국어 명사구로 쓰고, 한 커밋에는 하나의 논리적 변경만 담는다. 생성 파일과 수동 수정, 서로 독립적인 변경은 가능한 한 분리한다.

## 허용 type

`feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `style`, `build`, `ci`, `chore`, `revert`

## 이슈 연결

- 일반 작업 커밋 본문에는 이슈를 닫지 않는 `Refs: #123`을 사용한다.
- `dev → main` 배치 PR 본문에는 실제로 완료되는 각 이슈에 `Closes #123`을 사용한다.
- `dev` 대상 PR도 관련 `Refs #번호` 또는 `Closes #번호`를 포함한다.

예시:

```text
feat: 사용자 검색 추가

검색 조건과 빈 결과 화면을 추가한다.

Refs: #123
```

feature 브랜치를 `dev`로, `dev`를 `main`으로 병합할 때는 squash 또는 rebase merge를 사용하지 않는다. **merge commit 보존 규칙**에 따라 merge commit을 사용해 이슈별 논리적 커밋을 유지한다.
