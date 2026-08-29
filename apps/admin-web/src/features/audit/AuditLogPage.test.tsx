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
    content: [{ id: "audit-1", result: "SUCCESS", statusCode: 200,
      ipAddress: "127.0.0.1", userAgent: "browser", traceId: "trace-1",
      createdAt: "2026-08-24T00:00:00Z" }],
    page: 0, size: 20, totalElements: 1, totalPages: 1,
  });
});

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

test("shows access history and applies filters", async () => {
  renderPage();
  expect(await screen.findByText("127.0.0.1")).toBeInTheDocument();
  expect(screen.getByText(/browser/)).toBeInTheDocument();
  expect(screen.getByText("성공")).toBeInTheDocument();

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
