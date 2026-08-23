status: DONE_WITH_CONCERNS

files changed:
- `features/identity/src/main/java/com/ino/admin/identity/api/UserDirectoryUseCase.java`
- `features/identity/src/main/java/com/ino/admin/identity/application/UserDirectoryService.java`
- `features/identity/src/main/java/com/ino/admin/identity/infrastructure/persistence/UserRepository.java`
- `features/identity/src/test/java/com/ino/admin/identity/application/UserDirectoryServiceTest.java`
- `.superpowers/sdd/2026-08-23-user-directory-query/task-1-report.md`

RED command and exact failure reason:
- Command: `.\gradlew.bat :features:identity:test --tests "com.ino.admin.identity.application.UserDirectoryServiceTest"`
- Result: FAIL
- Exact failure reason: `compileTestJava` failed because `UserDirectoryUseCase` did not yet define `UserQuery`, `UserSort`, or `SortDirection`, and `UserRepository.search` still accepted only `(String, Pageable)`, so the new test code could not compile.

GREEN command and result:
- Command: `.\gradlew.bat :features:identity:test --tests "com.ino.admin.identity.application.UserDirectoryServiceTest"`
- Result: PASS (`BUILD SUCCESSFUL`)
- Additional compatibility check: `.\gradlew.bat :apps:admin-server:compileJava` PASS (`BUILD SUCCESSFUL`)

commit SHA:
- PENDING

self-review findings and concerns:
- Findings: Added typed query/sort/direction contract, service-side normalization for query/role/status, repository filtering for role/status, and stable secondary sort by `id ASC`.
- Findings: Kept the old `findUsers(String, int, int)` signature as a default bridge to avoid breaking dependent modules before Task 2 updates the controller.
- Concerns: GitHub Issue milestone assignment and `Dev CI` were not verifiable from this environment, so completion is local-verification only.
