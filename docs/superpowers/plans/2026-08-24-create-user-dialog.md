# Create User Dialog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the always-visible user creation card with an accessible permission-aware dialog.

**Architecture:** Keep `UsersPage` responsible for directory queries and move creation form state, validation, and mutation UI into `CreateUserDialog`. Reuse the existing `createUser` API and query keys; server authorization remains authoritative.

**Tech Stack:** React 18, TypeScript, TanStack Query, shadcn/ui Dialog, React Testing Library, Vitest, i18next

**Spec:** `docs/superpowers/specs/2026-08-24-operations-ux-monitoring-design.md`

## Global Constraints

- Start only after Issue #57 Dev CI succeeds.
- Follow `apps/admin-web/AGENTS.md`: labelled controls, keyboard focus, explicit loading/error states, and server-side authorization.
- Do not change the public create-user API contract.
- Write UI tests before moving production JSX.

---

### Task 1: Add the dialog primitive and creation component

**Files:**
- Create: `apps/admin-web/src/components/ui/dialog.tsx`
- Create: `apps/admin-web/src/features/users/component/CreateUserDialog.tsx`
- Create: `apps/admin-web/src/features/users/component/CreateUserDialog.test.tsx`
- Modify: `apps/admin-web/src/i18n/resources.ts`

**Interfaces:**
- Consumes: `createUser(input)`, `RoleOption[]`, `userKeys.all`
- Produces: `CreateUserDialog({ roles }: { roles: Array<{ value: string; label: string }> })`

- [ ] **Step 1: Add failing interaction tests**

Mock `createUser` and render the component inside the existing QueryClient/i18n test wrapper. Verify:

```ts
await user.click(screen.getByRole("button", { name: "사용자 추가" }))
expect(screen.getByRole("dialog", { name: "사용자 생성" })).toBeVisible()
expect(screen.getByLabelText("이름")).toHaveFocus()
```

Also test Escape/cancel focus return, invalid email/password rejection, server error retention, disabled submit while pending, and success closing the dialog while invalidating `userKeys.all`.

- [ ] **Step 2: Run the component test and observe RED**

Run: `npm.cmd test -- src/features/users/component/CreateUserDialog.test.tsx`

Working directory: `apps/admin-web`

Expected: FAIL because the dialog and component do not exist.

- [ ] **Step 3: Add the shadcn Dialog primitive**

Run from `apps/admin-web`: `npx.cmd shadcn@latest add dialog`

Review the generated file and retain repository import aliases and styling conventions. Do not modify unrelated shadcn components.

- [ ] **Step 4: Implement `CreateUserDialog`**

Use local `open` and `error` state plus `useMutation({ mutationFn: createUser })`. On success:

```ts
await queryClient.invalidateQueries({ queryKey: userKeys.all })
setOpen(false)
formRef.current?.reset()
toast.add({ title: t("created", { name: created.displayName }) })
```

Prevent `onOpenChange(false)` while pending. Give every input a stable label and render API errors inside an alert in the dialog body.

- [ ] **Step 5: Add Korean and English copy**

Add `addUser`, `createTitle`, `createDescription`, `cancel`, `creating`, and creation-error strings to both locale sections. Update `resources.test.ts` only if its namespace/key parity assertion requires explicit coverage.

- [ ] **Step 6: Re-run the component test and observe GREEN**

Run the command from Step 2.

Expected: PASS for accessibility, mutation, error, and success cases.

### Task 2: Integrate the dialog into the user directory

**Files:**
- Modify: `apps/admin-web/src/features/users/UsersPage.tsx`
- Modify: `apps/admin-web/src/features/users/UsersPage.test.tsx`

**Interfaces:**
- Consumes: `CreateUserDialog`, active non-`SUPER_ADMIN` role options
- Produces: permission-gated dialog trigger without changing list query behavior

- [ ] **Step 1: Change page tests first**

Assert that a principal with `user:create` sees `사용자 추가` but not the old inline `사용자 생성` card, and a principal without `user:create` sees no trigger. Preserve existing filter, pagination, edit, and status tests.

- [ ] **Step 2: Run the page test and observe RED**

Run: `npm.cmd test -- src/features/users/UsersPage.test.tsx`

Working directory: `apps/admin-web`

Expected: FAIL because the inline card still renders.

- [ ] **Step 3: Remove creation state from `UsersPage`**

Delete the page-level `createError`, `create` mutation, `submit` handler, and inline creation card. Render:

```tsx
{currentUser?.permissions.includes("user:create") && (
  <CreateUserDialog roles={roleOptions} />
)}
```

Keep role catalog loading behavior and all directory/edit state unchanged.

- [ ] **Step 4: Run focused and full frontend verification**

From `apps/admin-web`, run:

```powershell
npm.cmd test -- src/features/users/component/CreateUserDialog.test.tsx src/features/users/UsersPage.test.tsx
npm.cmd run lint
npm.cmd run typecheck
npm.cmd run test
npm.cmd run build
```

Expected: all commands PASS.

- [ ] **Step 5: Commit, push, and verify Dev CI**

```powershell
git add apps/admin-web/src/components/ui/dialog.tsx apps/admin-web/src/features/users/component/CreateUserDialog.tsx apps/admin-web/src/features/users/component/CreateUserDialog.test.tsx apps/admin-web/src/features/users/UsersPage.tsx apps/admin-web/src/features/users/UsersPage.test.tsx apps/admin-web/src/i18n/resources.ts
git commit -m "feat: 사용자 생성 모달 도입" -m "Refs: #58"
git push origin dev
gh run list --workflow "Dev CI" --branch dev --limit 5
```

After the matching Dev CI succeeds, leave #58 open and mark it ready for the Milestone's future `dev → main` batch PR; that batch PR owns `Closes #58`. Do not start #59 on a failed or unobserved run.
