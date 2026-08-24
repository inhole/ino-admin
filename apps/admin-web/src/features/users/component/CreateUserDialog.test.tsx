import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { createUser } from "@/features/users/api/usersApi";
import { CreateUserDialog } from "@/features/users/component/CreateUserDialog";
import { userKeys } from "@/features/users/hook/userKeys";

vi.mock("@/features/users/api/usersApi", () => ({
  createUser: vi.fn(),
}));

const mockedCreateUser = vi.mocked(createUser);
const roles = [
  { value: "ADMIN", label: "관리자" },
  { value: "VIEWER", label: "조회자" },
];

function renderDialog() {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

  render(
    <QueryClientProvider client={queryClient}>
      <CreateUserDialog roles={roles} />
    </QueryClientProvider>,
  );

  return { invalidateQueries };
}

function openDialog() {
  fireEvent.click(screen.getByRole("button", { name: "사용자 추가" }));
}

function fillValidForm() {
  fireEvent.change(screen.getByLabelText("이름"), {
    target: { value: "김관리" },
  });
  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "kim@example.com" },
  });
  fireEvent.change(screen.getByLabelText("초기 비밀번호"), {
    target: { value: "safe-password-1" },
  });
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CreateUserDialog", () => {
  test("사용자 추가를 선택하면 생성 다이얼로그를 열고 이름 입력란에 초점을 둔다", async () => {
    renderDialog();

    openDialog();

    expect(screen.getByRole("dialog", { name: "사용자 생성" })).toBeVisible();
    await waitFor(() => expect(screen.getByLabelText("이름")).toHaveFocus());
  });

  test("Escape와 취소는 다이얼로그를 닫고 추가 버튼으로 초점을 되돌린다", async () => {
    renderDialog();
    const trigger = screen.getByRole("button", { name: "사용자 추가" });

    openDialog();
    fireEvent.keyDown(document, { key: "Escape" });

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();

    openDialog();
    fireEvent.click(screen.getByRole("button", { name: "취소" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  test("유효하지 않은 이메일과 짧은 초기 비밀번호를 제출하지 않는다", () => {
    renderDialog();
    openDialog();

    fireEvent.change(screen.getByLabelText("이메일"), {
      target: { value: "not-an-email" },
    });
    fireEvent.change(screen.getByLabelText("초기 비밀번호"), {
      target: { value: "short" },
    });

    fireEvent.submit(screen.getByRole("form"));

    expect(screen.getByLabelText("이메일")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("초기 비밀번호")).toHaveAttribute("aria-invalid", "true");
    expect(mockedCreateUser).not.toHaveBeenCalled();
  });

  test("서버 오류가 나도 작성한 입력값과 다이얼로그를 유지한다", async () => {
    mockedCreateUser.mockRejectedValueOnce(new Error("서버 오류"));
    renderDialog();
    openDialog();
    fillValidForm();

    fireEvent.submit(screen.getByRole("form"));

    expect(await screen.findByRole("alert")).toHaveTextContent("사용자를 생성할 수 없습니다.");
    expect(screen.getByRole("dialog")).toBeVisible();
    expect(screen.getByLabelText("이름")).toHaveValue("김관리");
  });

  test("생성 요청 중에는 제출과 닫기를 막는다", async () => {
    let resolveCreate: ((value: { id: string; email: string; displayName: string; status: string; role: string; createdAt: string }) => void) | undefined;
    mockedCreateUser.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveCreate = resolve;
      }),
    );
    renderDialog();
    openDialog();
    fillValidForm();

    fireEvent.submit(screen.getByRole("form"));

    expect(
      await screen.findByRole("button", { name: /생성 중/ }),
    ).toBeDisabled();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.getByRole("dialog")).toBeVisible();

    resolveCreate?.({
      id: "user-2",
      email: "kim@example.com",
      displayName: "김관리",
      status: "ACTIVE",
      role: "VIEWER",
      createdAt: "2026-08-24T00:00:00Z",
    });
  });

  test("성공하면 사용자 목록을 무효화하고 다이얼로그를 닫는다", async () => {
    mockedCreateUser.mockResolvedValueOnce({
      id: "user-2",
      email: "kim@example.com",
      displayName: "김관리",
      status: "ACTIVE",
      role: "VIEWER",
      createdAt: "2026-08-24T00:00:00Z",
    });
    const { invalidateQueries } = renderDialog();
    openDialog();
    fillValidForm();

    fireEvent.submit(screen.getByRole("form"));

    await waitFor(() =>
      expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: userKeys.all }),
    );
    expect(
      screen.queryByRole("dialog", { name: "사용자 생성" }),
    ).not.toBeInTheDocument();
  });
});
