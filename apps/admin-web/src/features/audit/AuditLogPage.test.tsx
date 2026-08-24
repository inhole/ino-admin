import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { AuditLogPage } from "@/features/audit/AuditLogPage";

const auditApi = vi.hoisted(() => ({ getAuditLogs: vi.fn() }));
vi.mock("@/features/audit/api/auditApi", () => auditApi);

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={queryClient}><AuditLogPage /></QueryClientProvider>);
}

beforeEach(() => {
  auditApi.getAuditLogs.mockReset();
  auditApi.getAuditLogs.mockResolvedValue({
    content: [{ id: "audit-1", actorId: "actor-1", action: "USER_UPDATE",
      resource: "/api/v1/users/user-1", result: "SUCCESS", statusCode: 200,
      ipAddress: "127.0.0.1", userAgent: "browser", traceId: "trace-1",
      createdAt: "2026-08-24T00:00:00Z" }],
    page: 0, size: 20, totalElements: 1, totalPages: 1,
  });
});

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

test("shows audit records and applies filters", async () => {
  renderPage();
  expect(await screen.findByText("/api/v1/users/user-1")).toBeInTheDocument();
  expect(screen.getByText("성공")).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("작업자 ID"), { target: { value: "actor-1" } });
  fireEvent.change(screen.getByLabelText("동작"), { target: { value: "USER_UPDATE" } });
  fireEvent.click(screen.getByRole("button", { name: "검색" }));

  await waitFor(() => expect(auditApi.getAuditLogs).toHaveBeenLastCalledWith(
    expect.objectContaining({ actorId: "actor-1", action: "USER_UPDATE" }),
  ));
});

test("shows an explicit empty state", async () => {
  auditApi.getAuditLogs.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  renderPage();
  expect(await screen.findByText("기록된 감사 로그가 없습니다.")).toBeInTheDocument();
});
