import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { ApiClientError } from "@/api/client";
import { DashboardPage } from "@/features/dashboard/DashboardPage";

const dashboardApi = vi.hoisted(() => ({
  getMonitoringSummary: vi.fn(),
  getSamples: vi.fn(),
}));

vi.mock("@/features/dashboard/api/dashboardApi", () => dashboardApi);

const firstSnapshot = {
  timestamp: "2026-08-24T00:00:00Z",
  systemCpuUsage: 0.25,
  processCpuUsage: 0.1,
  heapUsedBytes: 268_435_456,
  heapMaxBytes: 1_073_741_824,
  processUptimeSeconds: 3_600,
  liveThreads: 32,
  peakThreads: 48,
  httpRequestCount: 100,
  httpRequestDurationSeconds: 12,
  httpServerErrorCount: 2,
};

const secondSnapshot = {
  ...firstSnapshot,
  timestamp: "2026-08-24T00:00:05Z",
  systemCpuUsage: 0.5,
  processCpuUsage: 0.2,
  heapUsedBytes: 536_870_912,
  httpRequestCount: 115,
  httpRequestDurationSeconds: 15,
  httpServerErrorCount: 3,
};

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <DashboardPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.useFakeTimers();
  dashboardApi.getMonitoringSummary.mockReset();
  dashboardApi.getSamples.mockReset();
  dashboardApi.getSamples.mockResolvedValue({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  });
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.restoreAllMocks();
});

test("collects the first snapshot, then shows derived metrics after the five-second refresh", async () => {
  dashboardApi.getMonitoringSummary
    .mockResolvedValueOnce(firstSnapshot)
    .mockResolvedValueOnce(secondSnapshot);

  renderPage();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });
  expect(screen.getAllByText("수집 중")).not.toHaveLength(0);
  expect(screen.getByText("CPU")).toBeInTheDocument();
  expect(screen.getAllByText("힙 메모리")).not.toHaveLength(0);
  expect(screen.getByText("업타임")).toBeInTheDocument();
  expect(screen.getByText("스레드")).toBeInTheDocument();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(5_001);
    await vi.advanceTimersByTimeAsync(0);
  });
  expect(screen.getByText("3.00 TPS")).toBeInTheDocument();
  expect(screen.getByText("200.0 ms")).toBeInTheDocument();
  expect(screen.getByText("6.7%")).toBeInTheDocument();
  expect(dashboardApi.getMonitoringSummary).toHaveBeenCalledTimes(2);
});

test("labels unavailable meters without replacing the successful dashboard", async () => {
  dashboardApi.getMonitoringSummary.mockResolvedValue({
    ...firstSnapshot,
    systemCpuUsage: null,
    heapUsedBytes: null,
    heapMaxBytes: null,
  });

  renderPage();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  expect(screen.getAllByText("사용 불가")).not.toHaveLength(0);
  expect(screen.getByText("CPU")).toBeInTheDocument();
  expect(screen.getAllByText("힙 메모리")).not.toHaveLength(0);
});

test("keeps the dashboard error actionable with retry after an initial request fails", async () => {
  dashboardApi.getMonitoringSummary
    .mockRejectedValueOnce(new ApiClientError("관제 정보를 불러올 수 없습니다.", 503))
    .mockResolvedValueOnce(firstSnapshot);

  renderPage();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  expect(screen.getByRole("alert")).toHaveTextContent("관제 정보를 불러올 수 없습니다.");
  fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));

  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  expect(screen.getAllByText("수집 중")).not.toHaveLength(0);
  expect(dashboardApi.getMonitoringSummary).toHaveBeenCalledTimes(2);
});
