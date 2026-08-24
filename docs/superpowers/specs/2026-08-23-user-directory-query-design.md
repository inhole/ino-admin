# 사용자 디렉터리 조회와 페이지 탐색 설계

## 1. 배경과 목표

현재 사용자 관리 화면은 서버의 기본 20건만 조회하며 검색, 역할·상태 필터, 정렬, 페이지 이동을 제공하지 않는다. 서버 API도 자유 검색과 페이지 번호·크기만 지원해 운영자가 많은 사용자 중 필요한 계정을 안정적으로 찾기 어렵다.

이번 변경은 사용자 목록 조회를 하나의 vertical slice로 완성한다. 조회 조건을 서버에서 적용하고 URL에 보존하여 새로고침, 공유, 뒤로가기로 같은 결과를 복원한다. 생성·수정 UI와 일괄 작업은 포함하지 않는다.

## 2. 선택한 접근

`GET /api/v1/users`에 허용값이 명시된 query parameter를 추가한다. Controller는 HTTP 입력을 검증하고, identity feature에는 HTTP와 무관한 조회 객체를 전달한다. Repository는 조건 검색과 페이지 정렬을 수행한다.

프론트엔드는 URL query parameter를 조회 상태의 단일 기준으로 사용한다. 화면의 Toolbar와 Pagination은 URL을 변경하고, TanStack Query는 정규화된 조회 객체를 query key와 API 요청에 사용한다.

다음 대안은 사용하지 않는다.

- 현재 페이지의 클라이언트 필터링: 전체 결과와 개수가 부정확하다.
- Spring `Pageable` 직접 노출: 엔티티 필드와 임의 정렬을 공개 API에 결합한다.

## 3. API 계약

`GET /api/v1/users`는 다음 parameter를 지원한다.

| parameter | 기본값 | 계약 |
|---|---:|---|
| `query` | 빈 문자열 | 이름 또는 이메일 부분 일치, 대소문자 구분 없음, 최대 320자 |
| `role` | 없음 | 정확한 역할 키 일치 |
| `status` | 없음 | `ACTIVE`, `LOCKED`, `DISABLED` 중 하나 |
| `page` | `0` | 0 이상 |
| `size` | `20` | 1~100 |
| `sort` | `createdAt` | `createdAt`, `displayName`, `email`, `role`, `status` 중 하나 |
| `direction` | `desc` | `asc` 또는 `desc` |

응답은 기존 `content`, `page`, `size`, `totalElements`, `totalPages` 계약을 유지한다. 정렬은 요청 필드 다음에 `id ASC`를 항상 추가해 페이지 경계를 안정화한다. 지원하지 않는 상태·정렬·방향과 범위를 벗어난 페이지 크기는 `400 VALIDATION_ERROR`로 거부한다.

역할 필터는 현재 존재 여부와 관계없이 정규화된 역할 키의 정확한 일치로 처리한다. 존재하지 않는 역할은 빈 결과를 반환하므로 역할 삭제와 조회 사이의 결합을 만들지 않는다.

## 4. 서버 구조

`UserController`는 문자열 parameter를 받아 `UserDirectoryUseCase.UserQuery`로 변환한다. 조회 객체는 검색어, 선택적 역할·상태, 페이지, 크기, 정렬 enum, 방향 enum을 가진다. enum 파싱과 오류 변환은 애플리케이션 경계에서 수행해 Repository에 임의 속성명이 전달되지 않게 한다.

`UserDirectoryService`는 입력을 정규화하고 허용된 enum을 JPA 속성명으로 명시적으로 매핑한다. `UserRepository`는 검색어·역할·상태의 선택 조건을 하나의 query에 적용하며 count query를 통해 전체 개수를 반환한다. Entity는 API에 노출하지 않고 기존 `UserSummary`로 변환한다.

`GET /api/v1/users/**`의 `user:read` 서버 인가는 그대로 유지한다. 필터나 메뉴 노출은 인가 수단으로 사용하지 않는다.

## 5. 프론트엔드 구조와 UX

`usersApi`는 `UserListQuery`를 받아 기본값과 빈 값을 제외한 URLSearchParams를 만든다. 정규화 함수는 URL 입력을 안전한 기본값으로 변환하고 동일한 객체를 TanStack Query key에 사용한다.

사용자 화면 상단 Toolbar는 다음을 제공한다.

- 이름·이메일 검색
- 역할 필터
- 상태 필터
- 정렬 필드와 방향
- 모든 조회 조건 초기화

검색 입력은 300ms debounce 후 URL에 반영한다. 역할·상태·정렬 변경은 즉시 반영한다. 검색·필터·정렬 변경 시 `page`는 0으로 초기화한다. 기본값은 URL에서 생략하고, 첫 페이지는 `page`를 생략한다.

목록 하단에는 전체 사용자 수, 현재 페이지/전체 페이지, 이전·다음 버튼을 표시한다. 존재하지 않는 높은 페이지를 조회해 빈 결과를 받으면 가능한 마지막 페이지로 한 번 보정한다. 요청 중에는 현재 데이터를 유지하고 목록 영역에 진행 상태를 표시해 화면 점프를 줄인다.

검색 결과가 없으면 “조건에 맞는 사용자가 없습니다”와 필터 초기화 action을 제공한다. 전체 사용자가 없으면 기존 빈 상태를 유지한다. 403은 공통 권한 없음 문구, 400은 조회 조건 오류, 나머지 API 오류는 서버 메시지와 문의 코드 및 재시도를 표시한다.

데스크톱에서는 기존 Table을 유지한다. 모바일에서는 가로 스크롤에 의존하지 않고 각 사용자를 이름·이메일 중심의 세로 요약 목록으로 표시한다. 이번 변경에서는 생성·수정 form이나 행 작업의 동작을 바꾸지 않는다.

## 6. 테스트 전략

테스트는 구현보다 먼저 작성하고 실제 RED를 확인한다.

서버 테스트:

- 이름·이메일 검색과 역할·상태 조합
- 각 허용 정렬과 안정적인 보조 정렬
- 기본값, 페이지 이동, 전체 개수
- 잘못된 status, sort, direction, size의 `400 VALIDATION_ERROR`
- `user:read`가 없는 요청의 403 유지

프론트 테스트:

- API query serialization과 기본값 생략
- URL에서 조회 상태 복원
- 검색 debounce와 필터·정렬 변경 시 첫 페이지 초기화
- 이전·다음 페이지 이동과 초기화
- 필터 결과 없음, 로딩, 400, 403, trace ID 오류 상태
- 데스크톱 Table과 모바일 요약의 핵심 정보

E2E 테스트:

- SUPER_ADMIN이 검색·필터 후 페이지를 이동하고 URL 상태를 복원한다.
- VIEWER가 `/users`에 직접 접근했을 때 서버 403과 문의 코드를 본다.

검증 명령은 영향받은 서버 테스트, `:apps:admin-server:architectureTest`, 프론트 `lint`, `typecheck`, `test`, `build`, 관련 Playwright 시나리오다. 최종 완료 판정은 feature PR과 dev 병합 뒤 `Dev CI` 결과로 한다.

## 7. 전달과 비범위

공개 API 변경이므로 `codex/54-user-directory-query`에서 작업하고 `dev` 대상 PR을 merge commit으로 병합한다. 일반 커밋은 `Refs: #54`를 사용한다.

다음은 후속 이슈로 분리한다.

- 사용자 생성·수정 Sheet
- 행 작업 DropdownMenu와 상태 변경 AlertDialog 재구성
- 선택 기반 일괄 작업
- 저장된 필터와 열 표시 설정
