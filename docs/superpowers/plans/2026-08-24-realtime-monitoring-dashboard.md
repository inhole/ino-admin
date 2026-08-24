# Realtime Monitoring Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show secured live resource and HTTP performance metrics with a browser-session 30-minute history.

**Architecture:** A server-owned `/api/v1/monitoring/summary` adapter reads Micrometer meters and exposes a stable nullable snapshot DTO without exposing Actuator metrics. The web polls every five seconds, derives interval rates from cumulative counters, retains 360 points, and renders accessible cards and separate unit-specific charts.

**Tech Stack:** Spring Boot Actuator/Micrometer, Spring Security, Flyway, React, TanStack Query, Recharts via shadcn chart, Vitest, MockMvc

**Spec:** `docs/superpowers/specs/2026-08-24-operations-ux-monitoring-design.md`

## Global Constraints

- Start only after Issue #58 Dev CI succeeds.
- Create `codex/59-realtime-monitoring-dashboard` from current `dev`; migration and authorization changes require a feature branch and a merge-commit PR back to `dev`.
- Keep Actuator exposure at `health,info`; never expose raw `/actuator/metrics` publicly.
- Store no metric history on the server and return no sensitive or high-cardinality tags.
- Protect the API with `monitoring:read`; grant it to `SUPER_ADMIN` and `ADMIN`, not `VIEWER`.

---

### Task 1: Add monitoring permission and server authorization

**Files:**
- Create: `apps/admin-server/src/main/resources/db/migration/V15__add_monitoring_permission.sql`
- Modify: `apps/admin-server/src/main/java/com/ino/admin/auth/SecurityConfig.java`
- Modify: `apps/admin-server/src/test/java/com/ino/admin/identity/AuthFlowIntegrationTest.java`

**Interfaces:**
- Produces: permission key `monitoring:read` and GET authorization for `/api/v1/monitoring/**`

- [ ] **Step 1: Write failing RBAC integration tests**

Add requests proving no token returns 401, `VIEWER` returns 403, and a token with `monitoring:read` reaches the controller. Use a mocked/temporary endpoint response only through the real application context; do not weaken other matcher rules.

- [ ] **Step 2: Run the focused integration test and observe RED**

Run: `./gradlew.bat :apps:admin-server:integrationTest --tests "com.ino.admin.identity.AuthFlowIntegrationTest"`

Expected: the monitoring authorization scenario fails before the matcher and permission exist.

- [ ] **Step 3: Add the Flyway migration**

Insert `monitoring:read` into the permission catalog and role-permission rows for `SUPER_ADMIN` and `ADMIN` using the existing schema and conflict-handling conventions from V7–V11. Do not edit an applied migration.

- [ ] **Step 4: Add the security matcher**

Before the final authenticated fallback, add:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/monitoring/**")
.hasAuthority("monitoring:read")
```

### Task 2: Implement the Micrometer snapshot API

**Files:**
- Create: `apps/admin-server/src/main/java/com/ino/admin/monitoring/MonitoringSummary.java`
- Create: `apps/admin-server/src/main/java/com/ino/admin/monitoring/MonitoringSummaryService.java`
- Create: `apps/admin-server/src/main/java/com/ino/admin/monitoring/MonitoringController.java`
- Create: `apps/admin-server/src/test/java/com/ino/admin/monitoring/MonitoringSummaryServiceTest.java`
- Create: `apps/admin-server/src/test/java/com/ino/admin/monitoring/MonitoringControllerTest.java`

**Interfaces:**
- Produces: `MonitoringSummary(timestamp, systemCpuUsage, processCpuUsage, heapUsedBytes, heapMaxBytes, processUptimeSeconds, liveThreads, peakThreads, httpRequestCount, httpRequestDurationSeconds, httpServerErrorCount)`
- Produces: `GET /api/v1/monitoring/summary`

- [ ] **Step 1: Write failing meter adapter tests**

Use `SimpleMeterRegistry` and register representative gauges/timers. Assert exact DTO values, and assert missing meters yield `null` fields without throwing. Inject a fixed `Clock` and assert the UTC timestamp.

- [ ] **Step 2: Run the service test and observe RED**

Run: `./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.monitoring.MonitoringSummaryServiceTest"`

Expected: FAIL because the service and DTO do not exist.

- [ ] **Step 3: Implement nullable meter reads**

Read gauges by the standard names `system.cpu.usage`, `process.cpu.usage`, `jvm.memory.used`/`jvm.memory.max` with `area=heap`, `process.uptime`, `jvm.threads.live`, and `jvm.threads.peak`. Aggregate `http.server.requests` timers across tags into count, total seconds, and 5xx count. Return `null` for absent or non-finite values.

- [ ] **Step 4: Add failing controller contract tests**

With `@WebMvcTest(MonitoringController.class)`, mock the service and assert the JSON field names and nullable output. Include JWT authority tests for allowed and forbidden calls if the slice imports security.

- [ ] **Step 5: Implement the controller**

Create a read-only controller mapped to `/api/v1/monitoring` whose `GET /summary` returns only `MonitoringSummary`.

- [ ] **Step 6: Run monitoring backend tests**

```powershell
./gradlew.bat :apps:admin-server:test --tests "com.ino.admin.monitoring.*"
./gradlew.bat :apps:admin-server:integrationTest --tests "com.ino.admin.identity.AuthFlowIntegrationTest"
```

Expected: PASS.

### Task 3: Add snapshot derivation and bounded history

**Files:**
- Modify: `apps/admin-web/src/features/dashboard/api/dashboardApi.ts`
- Modify: `apps/admin-web/src/features/dashboard/hook/dashboardKeys.ts`
- Create: `apps/admin-web/src/features/dashboard/model/monitoringHistory.ts`
- Create: `apps/admin-web/src/features/dashboard/model/monitoringHistory.test.ts`

**Interfaces:**
- Produces: `getMonitoringSummary(): Promise<MonitoringSnapshot>`
- Produces: `appendMonitoringPoint(history, snapshot, maxPoints = 360): MonitoringPoint[]`

- [ ] **Step 1: Write failing pure-function tests**

Cover first snapshot (`tps`, latency, error rate are `null`), normal deltas, zero request delta, counter reset, non-positive time delta, nullable counters, and truncation from 361 to 360 points.

- [ ] **Step 2: Run the model test and observe RED**

Run from `apps/admin-web`: `npm.cmd test -- src/features/dashboard/model/monitoringHistory.test.ts`

Expected: FAIL because the model does not exist.

- [ ] **Step 3: Implement exact interval formulas**

Use:

```ts
tps = requestDelta / elapsedSeconds
averageResponseMs = durationDelta * 1000 / requestDelta
serverErrorRate = errorDelta * 100 / requestDelta
```

Return `null` when deltas cannot be safely calculated. Preserve raw resource values on every point and return `next.slice(-maxPoints)`.

- [ ] **Step 4: Add the API function and query key**

Map `/api/v1/monitoring/summary` to a strict TypeScript interface whose nullable fields exactly match the Java DTO.

- [ ] **Step 5: Run model and API tests**

Add an API URL assertion if `dashboardApi` tests are split from `client.test.ts`, then run the affected Vitest files. Expected: PASS.

### Task 4: Build cards and charts

**Files:**
- Create: `apps/admin-web/src/components/ui/chart.tsx`
- Create: `apps/admin-web/src/features/dashboard/component/MetricCard.tsx`
- Create: `apps/admin-web/src/features/dashboard/component/MonitoringCharts.tsx`
- Modify: `apps/admin-web/src/features/dashboard/DashboardPage.tsx`
- Create: `apps/admin-web/src/features/dashboard/DashboardPage.test.tsx`
- Modify: `apps/admin-web/src/i18n/resources.ts`
- Modify: `apps/admin-web/package.json`
- Modify: `apps/admin-web/package-lock.json`

**Interfaces:**
- Consumes: `MonitoringPoint[]`, `getMonitoringSummary`, `appendMonitoringPoint`
- Produces: cards and five unit-specific chart panels with loading/collecting/unavailable/error states

- [ ] **Step 1: Write failing dashboard tests**

Mock successive snapshots and fake timers. Assert a 5-second refetch interval, first-sample `수집 중`, CPU/heap/uptime/thread cards, derived TPS/latency/error values after the second sample, unavailable labels for null meters, and retry after API failure.

- [ ] **Step 2: Run the dashboard test and observe RED**

Run: `npm.cmd test -- src/features/dashboard/DashboardPage.test.tsx`

Expected: FAIL because monitoring UI is absent.

- [ ] **Step 3: Add the chart dependency and primitive**

From `apps/admin-web`, run:

```powershell
npm.cmd install recharts
npx.cmd shadcn@latest add chart
```

Review generated changes and keep only Recharts and the chart primitive required by this dashboard.

- [ ] **Step 4: Implement polling and bounded history**

Use `useQuery` with `refetchInterval: 5000` and `refetchIntervalInBackground: false`. Append each newly timestamped successful snapshot once; do not duplicate points on rerenders. Retain prior successful history on transient errors.

- [ ] **Step 5: Implement accessible presentation**

Render summary cards plus separate CPU, heap, TPS, latency, and 5xx charts. Each chart must have a visible title, unit-bearing tooltip, legend where multiple series exist, and a textual latest-value summary so color is not the only signal.

- [ ] **Step 6: Add Korean and English monitoring copy**

Add labels for every metric, unit, `수집 중`, `사용 불가`, stale/error, retry, and chart descriptions. Preserve locale key parity.

- [ ] **Step 7: Run full verification**

Run:

```powershell
./gradlew.bat :apps:admin-server:test
./gradlew.bat :apps:admin-server:integrationTest
npm.cmd run lint
npm.cmd run typecheck
npm.cmd run test
npm.cmd run build
git diff --check
```

Run npm commands from `apps/admin-web`. Expected: all checks PASS.

### Task 5: Deliver Issue #59 to `dev`

**Files:**
- Modify: `docs/api/error-codes.md` only if authorization/error documentation requires a new entry
- Modify: `README.md` to describe dashboard scope and the absence of long-term retention

**Interfaces:**
- Produces: reviewed feature branch and `dev` merge commit referencing #59

- [ ] **Step 1: Review security and scope**

Confirm `/actuator/metrics` remains unexposed, `VIEWER` receives 403, snapshot DTO contains no tags or identifiers, history is client-only and capped at 360, and V15 is the only migration change.

- [ ] **Step 2: Commit the implementation**

```powershell
git add apps/admin-server apps/admin-web README.md docs/api/error-codes.md
git commit -m "feat: 실시간 관제 대시보드 도입" -m "Refs: #59"
git push -u origin codex/59-realtime-monitoring-dashboard
```

- [ ] **Step 3: Open and merge the `dev` PR only after checks pass**

```powershell
gh pr create --base dev --head codex/59-realtime-monitoring-dashboard --title "feat: 실시간 관제 대시보드 도입" --body "## 변경 사항`n- Micrometer 기반 보안 snapshot API`n- 최근 30분 실시간 리소스·HTTP 차트`n- monitoring:read RBAC와 회귀 테스트`n`nRefs: #59"
gh pr checks --watch
```

Merge with a merge commit after all required checks pass and verify the resulting `dev` Dev CI. Leave #59 open for the Milestone's future `dev → main` batch PR, which owns `Closes #59`. Do not merge or report CI verification if Actions cannot be observed.
