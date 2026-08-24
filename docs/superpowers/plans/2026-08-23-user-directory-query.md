# 사용자 디렉터리 조회와 페이지 탐색 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 사용자 목록을 서버에서 검색·역할/상태 필터·정렬·페이지 조회하고 URL로 동일한 조회 상태를 복원할 수 있게 한다.

**Architecture:** `GET /api/v1/users`의 명시적 query 계약을 `UserDirectoryUseCase.UserQuery`로 전달하고 JPA 조건 조회와 허용된 정렬만 적용한다. 프론트는 URL query parameter를 단일 조회 상태로 사용하고, 정규화된 `UserListQuery`를 TanStack Query key와 API 요청에 공유한다.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, JUnit 5, Mockito, React 18, TypeScript, React Router, TanStack Query, shadcn/ui, Vitest, Testing Library, Playwright

**Spec:** `docs/superpowers/specs/2026-08-23-user-directory-query-design.md`

## Global Constraints

- 공개 REST endpoint는 `/api/v1` prefix와 기존 page response 필드를 유지한다.
- Entity를 API에 직접 노출하지 않는다.
- `sort`는 `createdAt`, `displayName`, `email`, `role`, `status`만 허용하고 모든 정렬에 `id ASC`를 보조 정렬로 추가한다.
- `status`는 `ACTIVE`, `LOCKED`, `DISABLED`; `direction`은 `asc`, `desc`; `size`는 1~100만 허용한다.
- URL이 조회 상태의 단일 기준이며 기본값과 빈 값, 첫 페이지는 URL에서 생략한다.
- 검색·필터·정렬 변경은 `page=0`으로 돌아가며 검색 입력 debounce는 300ms다.
- UI 필터와 메뉴 노출은 인가 수단이 아니며 서버의 `user:read` 검사를 유지한다.
- 테스트를 구현보다 먼저 작성하고 로컬 RED와 GREEN을 실제로 확인한다.
- 커밋 제목은 `type: 한글 변경사항`, 본문은 `Refs: #54`를 사용한다.

---

### Task 1: identity 사용자 조건 조회 계약

**Files:**
- Modify: `features/identity/src/main/java/com/ino/admin/identity/api/UserDirectoryUseCase.java`
- Modify: `features/identity/src/main/java/com/ino/admin/identity/application/UserDirectoryService.java`
- Modify: `features/identity/src/main/java/com/ino/admin/identity/infrastructure/persistence/UserRepository.java`
- Modify: `features/identity/src/test/java/com/ino/admin/identity/application/UserDirectoryServiceTest.java`

**Interfaces:**
- Consumes: `User`, `UserStatus`, Spring Data `Pageable`
- Produces: `UserDirectoryUseCase.UserQuery`, `UserSort`, `SortDirection`, `findUsers(UserQuery query)`

- [ ] **Step 1: 조회 조건과 정렬을 검증하는 실패 테스트를 작성한다**

기존 테스트를 `UserQuery` 호출로 바꾸고 다음 검증을 추가한다.

```java
@Test
void appliesFiltersAndStableSort() {
    var repository = mock(UserRepository.class);
    when(repository.search(eq("admin"), eq("ADMIN"), eq(UserStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(Page.empty());

    new UserDirectoryService(repository).findUsers(new UserQuery(
            " admin ", "ADMIN", "ACTIVE", 2, 10, "displayName", "asc"));

    var pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).search(eq("admin"), eq("ADMIN"), eq(UserStatus.ACTIVE), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    assertThat(pageable.getValue().getSort().toList())
            .extracting(Sort.Order::getProperty, Sort.Order::getDirection)
            .containsExactly(
                    tuple("displayName", Sort.Direction.ASC),
                    tuple("id", Sort.Direction.ASC));
}
```

- [ ] **Step 2: identity 단위 테스트를 실행해 RED를 확인한다**

Run: `./gradlew.bat :features:identity:test --tests "com.ino.admin.identity.application.UserDirectoryServiceTest"`

Expected: `UserQuery`와 4조건 `search`가 없어 컴파일 또는 assertion FAIL.

- [ ] **Step 3: 공개 use case 조회 타입을 추가한다**

```java
UserPage findUsers(UserQuery query);

record UserQuery(String query, String role, String status, int page, int size,
                 String sort, String direction) {}

enum UserSort {
    CREATED_AT("createdAt"), DISPLAY_NAME("displayName"), EMAIL("email"),
    ROLE("role"), STATUS("status");
    private final String property;
}

enum SortDirection { ASC, DESC }
```

`UserSort`에는 외부 문자열을 대소문자 구분 없이 허용값으로 변환하고, 실패 시 `BusinessException("INVALID_USER_SORT", ...)`을 던지는 factory를 둔다. 방향과 상태도 같은 방식으로 명시적으로 변환한다.

- [ ] **Step 4: Repository 조건 query와 service 매핑을 구현한다**

```java
@Query("""
        select user from User user
        where (:query = ''
           or lower(user.email) like lower(concat('%', :query, '%'))
           or lower(user.displayName) like lower(concat('%', :query, '%')))
          and (:role is null or user.role = :role)
          and (:status is null or user.status = :status)
        """)
Page<User> search(@Param("query") String query,
                  @Param("role") String role,
                  @Param("status") UserStatus status,
                  Pageable pageable);
```

service는 빈 role/status를 `null`로 정규화하고 `Sort.by(direction, sort.property()).and(Sort.by(ASC, "id"))`를 사용한다.

- [ ] **Step 5: identity 테스트 GREEN을 확인한다**

Run: `./gradlew.bat :features:identity:test --tests "com.ino.admin.identity.application.UserDirectoryServiceTest"`

Expected: 모든 `UserDirectoryServiceTest` PASS.

- [ ] **Step 6: identity 변경을 커밋한다**

```powershell
git add features/identity/src/main/java/com/ino/admin/identity/api/UserDirectoryUseCase.java features/identity/src/main/java/com/ino/admin/identity/application/UserDirectoryService.java features/identity/src/main/java/com/ino/admin/identity/infrastructure/persistence/UserRepository.java features/identity/src/test/java/com/ino/admin/identity/application/UserDirectoryServiceTest.java
git commit -m "feat: 사용자 조건 조회 계약 확장" -m "Refs: #54"
```

---

### Task 2: 사용자 목록 HTTP query 검증과 API 문서

**Files:**
- Modify: `apps/admin-server/src/main/java/com/ino/admin/user/UserController.java`
- Create: `apps/admin-server/src/test/java/com/ino/admin/user/UserControllerTest.java`
- Create: `docs/api/users.md`

**Interfaces:**
- Consumes: Task 1의 `UserDirectoryUseCase.UserQuery`
- Produces: `GET /api/v1/users?query=&role=&status=&page=&size=&sort=&direction=` HTTP 계약

- [ ] **Step 1: 기본값과 잘못된 입력의 실패 테스트를 작성한다**

`@WebMvcTest(UserController.class)`에서 `UserDirectoryUseCase`를 mock하고 다음 요청을 검증한다.

```java
mockMvc.perform(get("/api/v1/users")
        .param("query", "kim")
        .param("role", "ADMIN")
        .param("status", "ACTIVE")
        .param("page", "1")
        .param("size", "10")
        .param("sort", "displayName")
        .param("direction", "asc"))
    .andExpect(status().isOk());

verify(directory).findUsers(new UserQuery(
        "kim", "ADMIN", "ACTIVE", 1, 10, "displayName", "asc"));
```

`status=UNKNOWN`, `sort=passwordHash`, `direction=sideways`, `size=101`은 각각 `400`과 `VALIDATION_ERROR`를 기대한다.

- [ ] **Step 2: Controller 테스트 RED를 확인한다**

Run: `./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.user.UserControllerTest"`

Expected: 새 parameter가 전달되지 않거나 잘못된 enum 입력이 거부되지 않아 FAIL.

- [ ] **Step 3: Controller parameter와 검증을 구현한다**

```java
@GetMapping
UserPage findAll(
        @RequestParam(defaultValue = "") @Size(max = 320) String query,
        @RequestParam(defaultValue = "") @Size(max = 50) String role,
        @RequestParam(defaultValue = "")
        @Pattern(regexp = "^$|ACTIVE|LOCKED|DISABLED") String status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "createdAt")
        @Pattern(regexp = "createdAt|displayName|email|role|status") String sort,
        @RequestParam(defaultValue = "desc")
        @Pattern(regexp = "asc|desc") String direction) {
    return userDirectory.findUsers(new UserQuery(
            query, role, status, page, size, sort, direction));
}
```

`@Pattern`, `@Min`, `@Max`, `@Size` 위반은 기존 `GlobalExceptionHandler`의 constraint violation 경로를 통해 `400 VALIDATION_ERROR`로 변환한다. `UserControllerTest`는 네 종류의 잘못된 parameter에서 응답 JSON `code`가 정확히 `VALIDATION_ERROR`인지 확인한다.

- [ ] **Step 4: 사용자 API 문서를 작성한다**

`docs/api/users.md`에 parameter 표, 기본값, 허용 정렬, `id ASC` 보조 정렬, 400/403 계약과 page response 예시를 기록한다.

- [ ] **Step 5: Controller와 architecture 테스트 GREEN을 확인한다**

Run:

```powershell
./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.user.UserControllerTest"
./gradlew.bat :apps:admin-server:architectureTest
```

Expected: 두 명령 PASS.

- [ ] **Step 6: HTTP 계약을 커밋한다**

```powershell
git add apps/admin-server/src/main/java/com/ino/admin/user/UserController.java apps/admin-server/src/test/java/com/ino/admin/user/UserControllerTest.java docs/api/users.md
git commit -m "feat: 사용자 목록 조회 API 확장" -m "Refs: #54"
```

---

### Task 3: 프론트 사용자 query 정규화와 API 직렬화

**Files:**
- Modify: `apps/admin-web/src/features/users/api/usersApi.ts`
- Create: `apps/admin-web/src/features/users/api/usersApi.test.ts`
- Modify: `apps/admin-web/src/features/users/hook/userKeys.ts`
- Create: `apps/admin-web/src/features/users/hook/userListQuery.ts`
- Create: `apps/admin-web/src/features/users/hook/userListQuery.test.ts`

**Interfaces:**
- Consumes: Task 2의 HTTP parameter 계약
- Produces: `UserListQuery`, `parseUserListQuery(URLSearchParams)`, `toUserListSearchParams(UserListQuery)`, `getUsers(UserListQuery)`, `userKeys.list(query)`

- [ ] **Step 1: URL 정규화와 API 직렬화 실패 테스트를 작성한다**

```ts
test('normalizes supported URL values and omits defaults', () => {
  const query = parseUserListQuery(new URLSearchParams(
    'query=kim&role=ADMIN&status=ACTIVE&page=2&sort=email&direction=asc',
  ))
  expect(query).toEqual({
    query: 'kim', role: 'ADMIN', status: 'ACTIVE', page: 2, size: 20,
    sort: 'email', direction: 'asc',
  })
  expect(toUserListSearchParams({ ...query, page: 0, sort: 'createdAt', direction: 'desc' }).toString())
    .toBe('query=kim&role=ADMIN&status=ACTIVE')
})
```

API 테스트는 `getUsers(query)`가 `/api/v1/users?query=kim&role=ADMIN&status=ACTIVE&page=2&sort=email&direction=asc`를 요청하는지 확인한다.

- [ ] **Step 2: Vitest RED를 확인한다**

Run: `npm test -- src/features/users/api/usersApi.test.ts src/features/users/hook/userListQuery.test.ts --maxWorkers=1`

Expected: query module과 새 `getUsers` signature가 없어 FAIL.

- [ ] **Step 3: 조회 타입과 정규화 함수를 구현한다**

```ts
export type UserStatusFilter = '' | 'ACTIVE' | 'LOCKED' | 'DISABLED'
export type UserSort = 'createdAt' | 'displayName' | 'email' | 'role' | 'status'
export type SortDirection = 'asc' | 'desc'

export interface UserListQuery {
  query: string
  role: string
  status: UserStatusFilter
  page: number
  size: number
  sort: UserSort
  direction: SortDirection
}
```

숫자와 enum은 allowlist로 정규화하고 잘못된 URL 값은 기본값으로 바꾼다. `URLSearchParams` 생성 순서는 `query`, `role`, `status`, `page`, `size`, `sort`, `direction`으로 고정한다.

- [ ] **Step 4: API와 query key를 조회 객체 기반으로 변경한다**

```ts
export function getUsers(query: UserListQuery) {
  const params = toUserListSearchParams(query)
  const suffix = params.size ? `?${params}` : ''
  return request<PageResponse<UserSummary>>(`/api/v1/users${suffix}`)
}

export const userKeys = {
  all: ['users'] as const,
  list: (query: UserListQuery) => [...userKeys.all, 'list', query] as const,
}
```

- [ ] **Step 5: 프론트 query 테스트 GREEN을 확인한다**

Run: `npm test -- src/features/users/api/usersApi.test.ts src/features/users/hook/userListQuery.test.ts --maxWorkers=1`

Expected: 모든 대상 테스트 PASS.

- [ ] **Step 6: 프론트 query 계약을 커밋한다**

```powershell
git add apps/admin-web/src/features/users/api/usersApi.ts apps/admin-web/src/features/users/api/usersApi.test.ts apps/admin-web/src/features/users/hook/userKeys.ts apps/admin-web/src/features/users/hook/userListQuery.ts apps/admin-web/src/features/users/hook/userListQuery.test.ts
git commit -m "feat: 사용자 조회 URL 계약 추가" -m "Refs: #54"
```

---

### Task 4: 사용자 Toolbar, Pagination과 반응형 목록

**Files:**
- Modify: `apps/admin-web/src/features/users/UsersPage.tsx`
- Create: `apps/admin-web/src/features/users/UsersPage.test.tsx`
- Create: `apps/admin-web/src/features/users/component/UserListToolbar.tsx`
- Create: `apps/admin-web/src/features/users/component/UserListPagination.tsx`
- Create: `apps/admin-web/src/features/users/component/UserList.tsx`
- Modify: `apps/admin-web/src/i18n/resources.ts`

**Interfaces:**
- Consumes: Task 3의 `UserListQuery`, parse/serialize 함수, `userKeys.list`, `getUsers(query)`
- Produces: URL 기반 조회 UI와 desktop/mobile 사용자 목록

- [ ] **Step 1: URL 복원과 조회 상호작용 실패 테스트를 작성한다**

`MemoryRouter initialEntries`를 `/users?query=kim&role=ADMIN&status=ACTIVE&page=1`로 두고 다음을 검증한다.

```ts
expect(await screen.findByDisplayValue('kim')).toBeInTheDocument()
expect(screen.getByRole('combobox', { name: '역할' })).toHaveValue('ADMIN')
expect(screen.getByText('2 / 3 페이지')).toBeInTheDocument()

fireEvent.change(screen.getByRole('combobox', { name: '상태' }), {
  target: { value: 'DISABLED' },
})
expect(screen.getByTestId('location')).toHaveTextContent(
  'query=kim&role=ADMIN&status=DISABLED',
)
expect(screen.getByTestId('location')).not.toHaveTextContent('page=1')
```

fake timer로 검색 입력 후 299ms에는 URL이 유지되고 300ms에 변경되는지, 초기화 버튼과 이전·다음 버튼도 별도 테스트한다.

- [ ] **Step 2: UI 테스트 RED를 확인한다**

Run: `npm test -- src/features/users/UsersPage.test.tsx --maxWorkers=1`

Expected: Toolbar, Pagination, URL 상태 UI가 없어 FAIL.

- [ ] **Step 3: shadcn 문서와 설치 상태를 확인한다**

Run:

```powershell
npx shadcn@latest info
npx shadcn@latest docs input select button table empty skeleton
```

기존 설치된 primitive만 사용하고 `src/components/ui`를 덮어쓰지 않는다.

- [ ] **Step 4: Toolbar와 URL 변경을 구현한다**

`UserListToolbar`는 검색 local state와 300ms debounce를 소유한다. 부모는 `setSearchParams(toUserListSearchParams({ ...next, page: 0 }))`로 URL을 갱신한다. 역할 목록은 활성 역할 catalog를 사용하며 “전체 역할” 빈 option을 제공한다.

- [ ] **Step 5: 서버 query와 페이지 보정을 연결한다**

```ts
const [searchParams, setSearchParams] = useSearchParams()
const query = parseUserListQuery(searchParams)
const users = useQuery({
  queryKey: userKeys.list(query),
  queryFn: () => getUsers(query),
  placeholderData: keepPreviousData,
})
```

응답이 빈 페이지이고 `page > 0 && totalPages > 0`이면 `page=totalPages-1`로 한 번 replace한다. loading, filtered-empty, global-empty, 400, 403, 기타 오류는 설계 문구에 맞게 분기한다.

- [ ] **Step 6: Pagination과 반응형 목록을 구현한다**

`UserListPagination`은 전체 건수, `page + 1 / totalPages`, 이전·다음 버튼을 렌더링하고 경계 버튼을 disabled 처리한다. `UserList`는 `md` 이상 Table, 작은 화면에서는 이름·이메일·역할·상태·등록일을 `Item` 기반 세로 목록으로 렌더링한다. 기존 수정/상태 action은 두 표현 모두에서 같은 callback을 사용한다.

- [ ] **Step 7: UI 테스트와 전체 프론트 검증 GREEN을 확인한다**

Run:

```powershell
npm test -- src/features/users/UsersPage.test.tsx --maxWorkers=1
npm run lint
npm run typecheck
npm test -- --maxWorkers=1
npm run build
```

Expected: 모든 명령 exit 0. 기존 Fast Refresh 경고는 별도 기록한다.

- [ ] **Step 8: 사용자 조회 UI를 커밋한다**

```powershell
git add apps/admin-web/src/features/users apps/admin-web/src/i18n/resources.ts
git commit -m "feat: 사용자 목록 조회 UX 개선" -m "Refs: #54"
```

---

### Task 5: RBAC E2E, 로드맵과 feature PR 전달

**Files:**
- Modify: `apps/admin-web/tests/e2e/rbac.spec.ts`
- Modify: `docs/architecture/admin-ui-ux-roadmap.md`

**Interfaces:**
- Consumes: Task 1~4의 서버 query와 URL 기반 UI
- Produces: SUPER_ADMIN 조회 흐름, VIEWER 403 회귀, `dev` 대상 PR

- [ ] **Step 1: SUPER_ADMIN 조회 E2E 실패 시나리오를 작성한다**

route mock이 query parameter를 읽어 다음을 반환하도록 만들고 테스트한다.

```ts
await page.getByPlaceholder('이름 또는 이메일 검색').fill('kim')
await page.getByRole('combobox', { name: '역할' }).selectOption('ADMIN')
await expect(page).toHaveURL(/query=kim/)
await expect(page).toHaveURL(/role=ADMIN/)
await expect(page.getByText('kim@example.com')).toBeVisible()
await page.getByRole('button', { name: '다음 페이지' }).click()
await expect(page).toHaveURL(/page=1/)
await page.reload()
await expect(page.getByDisplayValue('kim')).toBeVisible()
```

기존 VIEWER 직접 접근 403·문의 코드 시나리오는 유지한다.

- [ ] **Step 2: E2E RED를 확인한다**

Run: `npm run test:e2e -- tests/e2e/rbac.spec.ts --grep "사용자 조회|VIEWER"`

Expected: 새 Toolbar와 페이지 이동이 없어 SUPER_ADMIN 시나리오 FAIL.

- [ ] **Step 3: E2E fixture를 완성하고 GREEN을 확인한다**

Run: `npm run test:e2e -- tests/e2e/rbac.spec.ts`

Expected: RBAC spec 전체 PASS.

- [ ] **Step 4: UI 로드맵을 갱신한다**

다음 항목만 완료 처리한다.

```markdown
- [x] 검색, 역할·상태 필터, 정렬, 초기화 Toolbar
- [x] URL query parameter 기반 조회 상태 유지
- [x] 서버 페이지네이션과 `Pagination` 적용
- [x] 데스크톱 Data Table과 모바일 사용자 요약 표현
```

생성·수정 Sheet, 행별 DropdownMenu, 일괄 작업은 미완료로 유지한다.

- [ ] **Step 5: 전체 변경을 최종 검증한다**

Run:

```powershell
./gradlew.bat :features:identity:test
./gradlew.bat :apps:admin-server:test
./gradlew.bat :apps:admin-server:architectureTest
Set-Location apps/admin-web
npm run lint
npm run typecheck
npm test -- --maxWorkers=1
npm run build
npm run test:e2e -- tests/e2e/rbac.spec.ts
Set-Location ../..
git diff --check dev...HEAD
git status --short
```

Expected: 모든 검증 exit 0, 계획 파일과 #54 범위 파일만 변경됨.

- [ ] **Step 6: E2E와 문서를 커밋한다**

```powershell
git add apps/admin-web/tests/e2e/rbac.spec.ts docs/architecture/admin-ui-ux-roadmap.md
git commit -m "test: 사용자 조회 흐름 검증 보강" -m "Refs: #54"
```

- [ ] **Step 7: feature 브랜치를 push하고 dev 대상 PR을 연다**

```powershell
git push -u origin codex/54-user-directory-query
gh pr create --base dev --head codex/54-user-directory-query --title "feat: 사용자 디렉터리 조회 개선" --body "## 변경 사항`n- 사용자 검색·역할/상태 필터·정렬 API`n- URL 기반 페이지 조회 UI`n- 반응형 사용자 목록과 RBAC 회귀 테스트`n`nRefs #54"
```

- [ ] **Step 8: Dev CI와 리뷰 후 merge commit으로 dev에 병합한다**

`Dev CI`의 모든 job과 리뷰를 확인한다. 실패하면 feature 브랜치에서 수정하고 재검증한다. 통과 후 squash/rebase가 아닌 merge commit으로 병합한다. Issue는 `dev → main` 배치 PR에서 `Closes #54`로 닫으므로 feature PR에서는 닫지 않는다.
