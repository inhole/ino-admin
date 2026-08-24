import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { MenuDialog } from "@/features/menus/component/MenuDialog";

afterEach(cleanup);

describe("MenuDialog", () => {
  test("추가 폼을 모달에서 제출한다", async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<MenuDialog onSave={onSave} pending={false} />);
    fireEvent.click(screen.getByRole("button", { name: "메뉴 추가" }));
    fireEvent.change(screen.getByLabelText("메뉴 ID"), { target: { value: "reports" } });
    fireEvent.change(screen.getByLabelText("메뉴 이름"), { target: { value: "보고서" } });
    fireEvent.change(screen.getByLabelText("경로"), { target: { value: "/reports" } });
    fireEvent.change(screen.getByLabelText("아이콘"), { target: { value: "menu" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ id: "reports", label: "보고서", route: "/reports" })));
  });
});
