import { describe, expect, test } from "vitest";
import {
  appendMonitoringPoint,
  type MonitoringSnapshot,
} from "@/features/dashboard/model/monitoringHistory";

function snapshot(
  overrides: Partial<MonitoringSnapshot> = {},
): MonitoringSnapshot {
  return {
    timestamp: "2026-08-24T00:00:00Z",
    systemCpuUsage: 0.25,
    processCpuUsage: 0.1,
    heapUsedBytes: 1024,
    heapMaxBytes: 4096,
    processUptimeSeconds: 60,
    liveThreads: 12,
    peakThreads: 14,
    httpRequestCount: 100,
    httpRequestDurationSeconds: 12.5,
    httpServerErrorCount: 2,
    ...overrides,
  };
}

describe("appendMonitoringPoint", () => {
  test("starts history with null interval metrics", () => {
    const result = appendMonitoringPoint([], snapshot());

    expect(result).toEqual([
      expect.objectContaining({
        tps: null,
        averageResponseMs: null,
        serverErrorRate: null,
        systemCpuUsage: 0.25,
      }),
    ]);
  });

  test("derives interval metrics from safe cumulative counter deltas", () => {
    const history = appendMonitoringPoint([], snapshot());
    const result = appendMonitoringPoint(
      history,
      snapshot({
        timestamp: "2026-08-24T00:00:05Z",
        httpRequestCount: 120,
        httpRequestDurationSeconds: 15.5,
        httpServerErrorCount: 3,
      }),
    );

    expect(result.at(-1)).toMatchObject({
      tps: 4,
      averageResponseMs: 150,
      serverErrorRate: 5,
    });
  });

  test("keeps latency and error rate null when the request count does not increase", () => {
    const history = appendMonitoringPoint([], snapshot());
    const result = appendMonitoringPoint(
      history,
      snapshot({
        timestamp: "2026-08-24T00:00:05Z",
        httpRequestDurationSeconds: 15.5,
        httpServerErrorCount: 3,
      }),
    );

    expect(result.at(-1)).toMatchObject({
      tps: 0,
      averageResponseMs: null,
      serverErrorRate: null,
    });
  });

  test("returns null derived metrics after a cumulative counter reset", () => {
    const history = appendMonitoringPoint([], snapshot());
    const result = appendMonitoringPoint(
      history,
      snapshot({
        timestamp: "2026-08-24T00:00:05Z",
        httpRequestCount: 10,
        httpRequestDurationSeconds: 1.5,
        httpServerErrorCount: 0,
      }),
    );

    expect(result.at(-1)).toMatchObject({
      tps: null,
      averageResponseMs: null,
      serverErrorRate: null,
    });
  });

  test("returns null derived metrics when elapsed time is not positive", () => {
    const history = appendMonitoringPoint([], snapshot());
    const result = appendMonitoringPoint(
      history,
      snapshot({
        httpRequestCount: 120,
        httpRequestDurationSeconds: 15.5,
        httpServerErrorCount: 3,
      }),
    );

    expect(result.at(-1)).toMatchObject({
      tps: null,
      averageResponseMs: null,
      serverErrorRate: null,
    });
  });

  test("returns null only for derived metrics whose required counters are unavailable", () => {
    const history = appendMonitoringPoint(
      [],
      snapshot({ httpRequestDurationSeconds: null, httpServerErrorCount: null }),
    );
    const result = appendMonitoringPoint(
      history,
      snapshot({
        timestamp: "2026-08-24T00:00:05Z",
        httpRequestCount: 120,
        httpRequestDurationSeconds: null,
        httpServerErrorCount: null,
      }),
    );

    expect(result.at(-1)).toMatchObject({
      tps: 4,
      averageResponseMs: null,
      serverErrorRate: null,
    });
  });

  test("truncates 361 snapshots to the latest 360 by default", () => {
    let history = [] as ReturnType<typeof appendMonitoringPoint>;
    for (let index = 0; index < 361; index += 1) {
      history = appendMonitoringPoint(
        history,
        snapshot({
          timestamp: new Date(index * 5000).toISOString(),
          httpRequestCount: index,
        }),
      );
    }

    expect(history).toHaveLength(360);
    expect(history[0]?.timestamp).toBe("1970-01-01T00:00:05.000Z");
    expect(history.at(-1)?.timestamp).toBe("1970-01-01T00:30:00.000Z");
  });
});
