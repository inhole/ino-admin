import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { AccessHistoryPage } from "@/features/audit/AuditLogPage";

const auditApi = vi.hoisted(() => ({ getAccessHistory: vi.fn() }));
vi.mock("@/features/audit/api/auditApi", () => auditApi);

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={queryClient}><AccessHistoryPage /></QueryClientProvider>);
}

beforeEach(() => {
  auditApi.getAccessHistory.mockReset();
  auditApi.getAccessHistory.mockResolvedValue({
    content: [{ id: "audit-1", email: "admin@example.com", displayName: "관리자", role: "SUPER_ADMIN",
      createdAt: "2026-08-24T00:00:00Z" }],
    page: 0, size: 20, totalElements: 1, totalPages: 1,
  });
});

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

test("shows the administrator account, name, role, and login time", async () => {
  renderPage();
  expect(await screen.findByText("admin@example.com")).toBeInTheDocument();
  expect(screen.getByText("관리자")).toBeInTheDocument();
  expect(screen.getByText("SUPER_ADMIN")).toBeInTheDocument();
  expect(screen.getByText(/2026/)).toBeInTheDocument();
  expect(screen.queryByText("127.0.0.1")).not.toBeInTheDocument();
  expect(screen.queryByText("browser")).not.toBeInTheDocument();
  expect(screen.queryByText("성공")).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "검색" }));

  await waitFor(() => expect(auditApi.getAccessHistory).toHaveBeenLastCalledWith(
    expect.objectContaining({}),
  ));
});

test("shows an explicit empty state", async () => {
  auditApi.getAccessHistory.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  renderPage();
  expect(await screen.findByText("기록된 접속 이력이 없습니다.")).toBeInTheDocument();
});

test("requests the next page and disables the last-page button", async () => {
  auditApi.getAccessHistory.mockImplementation(({ page = 0 }: { page?: number }) => Promise.resolve({
    content: [{ id: `audit-${page}`, email: `admin${page}@example.com`, displayName: "관리자", role: "SUPER_ADMIN",
      createdAt: "2026-08-24T00:00:00Z" }],
    page, size: 20, totalElements: 21, totalPages: 2,
  }));
  renderPage();

  const next = await screen.findByRole("button", { name: "다음 페이지" });
  expect(screen.getByRole("button", { name: "이전 페이지" })).toBeDisabled();
  fireEvent.click(next);

  await waitFor(() => expect(auditApi.getAccessHistory).toHaveBeenLastCalledWith(
    expect.objectContaining({ page: 1, size: 20 }),
  ));
  expect(await screen.findByText("admin1@example.com")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "다음 페이지" })).toBeDisabled();
});
