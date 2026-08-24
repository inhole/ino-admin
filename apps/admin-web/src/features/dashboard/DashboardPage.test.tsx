import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { ApiClientError } from "@/api/client";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import i18n from "@/i18n";

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
  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    value: "visible",
  });
  vi.useRealTimers();
  vi.restoreAllMocks();
});

function metricCard(label: string) {
  const labelElement = screen
    .getAllByText(label)
    .find((element) => element.getAttribute("data-slot") === "card-description");
  const card = labelElement?.closest('[data-slot="card"]');
  if (!(card instanceof HTMLElement)) {
    throw new Error(`Metric card not found: ${label}`);
  }
  return within(card);
}

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

test("labels derived HTTP metrics unavailable when the source counters are absent", async () => {
  dashboardApi.getMonitoringSummary.mockResolvedValue({
    ...firstSnapshot,
    httpRequestCount: null,
    httpRequestDurationSeconds: null,
    httpServerErrorCount: null,
  });

  renderPage();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });

  expect(metricCard("TPS").getAllByText("사용 불가")).toHaveLength(2);
  expect(metricCard("평균 지연 시간").getAllByText("사용 불가")).toHaveLength(2);
  expect(metricCard("5xx 비율").getAllByText("사용 불가")).toHaveLength(2);
});

test("keeps derived metrics collecting when valid counters reset between snapshots", async () => {
  dashboardApi.getMonitoringSummary
    .mockResolvedValueOnce(firstSnapshot)
    .mockResolvedValueOnce({
      ...firstSnapshot,
      timestamp: "2026-08-24T00:00:05Z",
      httpRequestCount: 10,
      httpRequestDurationSeconds: 1,
      httpServerErrorCount: 0,
    });

  renderPage();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
    await vi.advanceTimersByTimeAsync(5_001);
    await vi.advanceTimersByTimeAsync(0);
  });

  expect(metricCard("TPS").getAllByText("수집 중")).not.toHaveLength(0);
  expect(metricCard("평균 지연 시간").getAllByText("수집 중")).not.toHaveLength(0);
  expect(metricCard("5xx 비율").getAllByText("수집 중")).not.toHaveLength(0);
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

test("retains history and announces stale status when a refresh fails after success", async () => {
  dashboardApi.getMonitoringSummary
    .mockResolvedValueOnce(firstSnapshot)
    .mockRejectedValueOnce(new ApiClientError("관제 정보를 불러올 수 없습니다.", 503));

  renderPage();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
    await vi.advanceTimersByTimeAsync(5_001);
    await vi.advanceTimersByTimeAsync(0);
  });

  expect(screen.getByRole("alert")).toHaveTextContent("마지막 수집 값을 표시하고 있습니다.");
  expect(screen.getByText("CPU")).toBeInTheDocument();
  expect(screen.queryByText("최신 스냅샷 반영됨")).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
});

test("stops interval polling while the document is hidden", async () => {
  dashboardApi.getMonitoringSummary.mockResolvedValue(firstSnapshot);
  renderPage();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });

  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    value: "hidden",
  });
  document.dispatchEvent(new Event("visibilitychange"));

  await act(async () => {
    await vi.advanceTimersByTimeAsync(10_000);
  });

  expect(dashboardApi.getMonitoringSummary).toHaveBeenCalledTimes(1);
});

test("names every monitoring chart as its own accessible region", async () => {
  dashboardApi.getMonitoringSummary.mockResolvedValue(firstSnapshot);
  renderPage();

  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });

  expect(screen.getByRole("region", { name: "CPU 사용률" })).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "힙 메모리" })).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "처리량" })).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "응답 지연 시간" })).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "5xx 비율" })).toBeInTheDocument();
});

test("localizes chart labels, loading status, and uptime units in English", async () => {
  await i18n.changeLanguage("en");
  dashboardApi.getMonitoringSummary.mockResolvedValue(firstSnapshot);
  renderPage();

  expect(screen.getByRole("status", { name: "Loading monitoring data…" })).toBeInTheDocument();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });

  expect(metricCard("Uptime").getByText("1 hr 0 min")).toBeInTheDocument();
  expect(screen.getAllByText("System CPU usage")).not.toHaveLength(0);
  expect(screen.getAllByText("Process CPU usage")).not.toHaveLength(0);
});
