import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeAll, describe, expect, test, vi } from "vitest";
import { CreateRoleDialog, EditRolePermissionsDialog } from "@/features/permissions/component/RoleDialogs";

afterEach(cleanup);

beforeAll(() => {
  if (!window.PointerEvent) {
    Object.defineProperty(window, "PointerEvent", { configurable: true, value: MouseEvent });
  }
});

describe("RoleDialogs", () => {
  test("역할을 모달에서 생성한다", async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<CreateRoleDialog onSave={onSave} pending={false} />);
    fireEvent.click(screen.getByRole("button", { name: "역할 생성" }));
    fireEvent.change(screen.getByLabelText("역할 키"), { target: { value: "EDITOR" } });
    fireEvent.change(screen.getByLabelText("역할 이름"), { target: { value: "편집자" } });
    fireEvent.click(screen.getByRole("button", { name: "역할 생성" }));
    await waitFor(() => expect(onSave).toHaveBeenCalledWith({ role: "EDITOR", displayName: "편집자", permissions: [] }));
  });

  test("역할 권한을 모달에서 수정한다", async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<EditRolePermissionsDialog available={["user:read", "menu:read"]} onSave={onSave} pending={false} role={{ role: "EDITOR", displayName: "편집자", systemRole: false, enabled: true, permissions: ["user:read"] }} />);
    fireEvent.click(screen.getByRole("button", { name: "수정" }));
    const checkbox = screen.getByRole("checkbox", { name: "menu:read" });
    fireEvent.click(checkbox);
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onSave).toHaveBeenCalledWith(["user:read", "menu:read"]));
  });
});
