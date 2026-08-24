# Bootstrap Admin UTF-8 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve a Korean bootstrap-admin display name from UTF-8 `.env` configuration through PostgreSQL persistence.

**Architecture:** Load the optional root `.env` exactly once as an explicitly UTF-8 Spring property source, then keep the existing bootstrap domain flow unchanged. Characterization tests cover configuration binding separately from the existing PostgreSQL round-trip test so the failing boundary is visible.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring ConfigurationProperties, JPA, PostgreSQL integration tests, JUnit 5

**Spec:** `docs/superpowers/specs/2026-08-24-operations-ux-monitoring-design.md`

## Global Constraints

- Work on Issue #57 from `dev`; this isolated configuration bug may be committed directly to `dev`.
- Never log the bootstrap password, email, or raw configuration values.
- Do not guess a source encoding or auto-convert already corrupted database values.
- Write tests before production changes and stop before Issue #58 until Dev CI passes.

---

### Task 1: Characterize UTF-8 `.env` binding

**Files:**
- Create: `apps/admin-server/src/test/java/com/ino/admin/config/AdminBootstrapPropertiesBindingTest.java`
- Modify: `apps/admin-server/src/main/resources/application.yml`
- Create: `apps/admin-server/src/main/java/com/ino/admin/config/Utf8DotenvConfig.java`

**Interfaces:**
- Consumes: `AdminBootstrapProperties#getDisplayName()`
- Produces: optional UTF-8 `file:.env` property source with OS/JVM environment variables retaining higher precedence

- [ ] **Step 1: Write the failing configuration test**

Use `ApplicationContextRunner` with a temporary UTF-8 `.env` containing `APP_BOOTSTRAP_ADMIN_DISPLAY_NAME=시스템 관리자`, register `Utf8DotenvConfig`, `ApplicationConfig`, and `AdminBootstrapProperties`, and assert:

```java
assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
        .isEqualTo("시스템 관리자");
```

Add a second case with no `.env` and assert the application default remains `시스템 관리자`.

- [ ] **Step 2: Run the focused test and observe RED**

Run: `./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.config.AdminBootstrapPropertiesBindingTest"`

Expected: FAIL because `.env` is still imported through the implicit properties loader or the explicit UTF-8 configuration does not exist.

- [ ] **Step 3: Add the explicit UTF-8 property source**

Create:

```java
@Configuration(proxyBeanMethods = false)
@PropertySource(value = "file:.env", ignoreResourceNotFound = true, encoding = "UTF-8")
class Utf8DotenvConfig {}
```

Remove `spring.config.import: optional:file:.env[.properties]` from `application.yml`. Keep `${APP_BOOTSTRAP_ADMIN_DISPLAY_NAME:시스템 관리자}` unchanged so environment variables override the file and the Korean default remains valid.

- [ ] **Step 4: Re-run the focused test and observe GREEN**

Run the command from Step 2.

Expected: PASS for UTF-8 `.env` and missing-file cases.

### Task 2: Verify persistence and document recovery

**Files:**
- Modify: `apps/admin-server/src/test/java/com/ino/admin/identity/UserPersistenceIntegrationTest.java`
- Modify: `docs/operations/admin-bootstrap.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: `AdminBootstrapService#bootstrap(String, String, String)`
- Produces: documented UTF-8 input and manual correction guidance

- [ ] **Step 1: Strengthen the PostgreSQL round-trip test**

Bootstrap `시스템 관리자 한글 검증`, clear the persistence context, reload by email, and assert the exact display name. The clear-and-reload step must prove a database round trip rather than a managed-entity echo.

- [ ] **Step 2: Run the focused integration test**

Run: `./gradlew.bat :apps:admin-server:integrationTest --tests "com.ino.admin.identity.UserPersistenceIntegrationTest"`

Expected: PASS against the configured integration PostgreSQL environment; if PostgreSQL is unavailable, report the test as not run rather than claiming success.

- [ ] **Step 3: Update operating instructions**

Document that `.env` must be UTF-8 without relying on console code pages, show the existing PowerShell environment-variable example, and state that an already corrupted display name must be corrected through user management or an operator-reviewed parameterized SQL update.

- [ ] **Step 4: Run regression checks**

Run:

```powershell
./gradlew.bat :features:identity:test --tests "com.ino.admin.identity.bootstrap.AdminBootstrapServiceTest"
./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.config.AdminBootstrapPropertiesBindingTest"
git diff --check
```

Expected: all executed checks PASS and no whitespace errors.

- [ ] **Step 5: Commit, push, and verify Dev CI**

```powershell
git add apps/admin-server/src/main/resources/application.yml apps/admin-server/src/main/java/com/ino/admin/config/Utf8DotenvConfig.java apps/admin-server/src/test/java/com/ino/admin/config/AdminBootstrapPropertiesBindingTest.java apps/admin-server/src/test/java/com/ino/admin/identity/UserPersistenceIntegrationTest.java docs/operations/admin-bootstrap.md README.md
git commit -m "fix: 초기 관리자 한글 설정 보존" -m "Refs: #57"
git push origin dev
gh run list --workflow "Dev CI" --branch dev --limit 5
```

Wait for the matching commit's Dev CI conclusion. After success, leave #57 open and mark it ready for the Milestone's future `dev → main` batch PR; that batch PR owns `Closes #57`. On failure, remain on #57 and fix the run.
