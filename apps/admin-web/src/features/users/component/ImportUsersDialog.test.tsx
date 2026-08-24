import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { importUsersExcel } from "@/features/users/api/usersApi";
import { ImportUsersDialog } from "@/features/users/component/ImportUsersDialog";

vi.mock("@/features/users/api/usersApi", () => ({
  downloadUserImportTemplate: vi.fn(),
  importUsersExcel: vi.fn(),
}));

const mockedImport = vi.mocked(importUsersExcel);

function renderDialog() {
  render(<QueryClientProvider client={new QueryClient()}><ImportUsersDialog /></QueryClientProvider>);
}

afterEach(() => { cleanup(); vi.clearAllMocks(); });

describe("ImportUsersDialog", () => {
  test("선택한 XLSX를 가져오고 성공 건수를 표시한다", async () => {
    mockedImport.mockResolvedValue({ createdCount: 2 });
    renderDialog();
    fireEvent.click(screen.getByRole("button", { name: "Excel 가져오기" }));
    const file = new File(["xlsx"], "users.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    fireEvent.change(screen.getByLabelText("XLSX 파일"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "사용자 가져오기" }));

    await waitFor(() => expect(mockedImport).toHaveBeenCalledWith(file));
    expect(await screen.findByText("사용자 2명을 가져왔습니다.")).toBeInTheDocument();
  });

  test("파일을 선택하지 않으면 요청하지 않는다", () => {
    renderDialog();
    fireEvent.click(screen.getByRole("button", { name: "Excel 가져오기" }));
    fireEvent.click(screen.getByRole("button", { name: "사용자 가져오기" }));
    expect(screen.getByText("가져올 XLSX 파일을 선택하세요.")).toBeInTheDocument();
    expect(mockedImport).not.toHaveBeenCalled();
  });
});
