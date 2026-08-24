import { afterEach, expect, test, vi } from "vitest";
import { getMonitoringSummary } from "@/features/dashboard/api/dashboardApi";

afterEach(() => vi.restoreAllMocks());

test("loads the nullable monitoring snapshot from the secured summary endpoint", async () => {
  const response = {
    timestamp: "2026-08-24T00:00:00Z",
    systemCpuUsage: 0.25,
    processCpuUsage: null,
    heapUsedBytes: 1024,
    heapMaxBytes: 4096,
    processUptimeSeconds: 60,
    liveThreads: 12,
    peakThreads: 14,
    httpRequestCount: 100,
    httpRequestDurationSeconds: 12.5,
    httpServerErrorCount: 2,
  };
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify(response), {
      headers: { "Content-Type": "application/json" },
    }),
  );

  await expect(getMonitoringSummary()).resolves.toEqual(response);
  expect(fetch).toHaveBeenCalledWith(
    "/api/v1/monitoring/summary",
    expect.objectContaining({ headers: expect.any(Headers) }),
  );
});
