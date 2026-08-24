export interface MonitoringSnapshot {
  timestamp: string;
  systemCpuUsage: number | null;
  processCpuUsage: number | null;
  heapUsedBytes: number | null;
  heapMaxBytes: number | null;
  processUptimeSeconds: number | null;
  liveThreads: number | null;
  peakThreads: number | null;
  httpRequestCount: number | null;
  httpRequestDurationSeconds: number | null;
  httpServerErrorCount: number | null;
}

export interface MonitoringPoint extends MonitoringSnapshot {
  tps: number | null;
  averageResponseMs: number | null;
  serverErrorRate: number | null;
}

export type HttpCounterKey =
  | "httpRequestCount"
  | "httpRequestDurationSeconds"
  | "httpServerErrorCount";

export type DerivedMetricStatus = "available" | "collecting" | "unavailable";

export function classifyDerivedMetric(
  latest: MonitoringPoint,
  previous: MonitoringPoint | undefined,
  requiredCounters: HttpCounterKey[],
  value: number | null,
): DerivedMetricStatus {
  const isUnavailable = (point: MonitoringPoint | undefined) =>
    point !== undefined && requiredCounters.some((counter) => point[counter] === null);

  if (isUnavailable(latest) || isUnavailable(previous)) return "unavailable";
  return value === null ? "collecting" : "available";
}

function counterDelta(current: number | null, previous: number | null) {
  if (
    current === null ||
    previous === null ||
    !Number.isFinite(current) ||
    !Number.isFinite(previous) ||
    current < 0 ||
    previous < 0
  ) {
    return null;
  }

  const delta = current - previous;
  return Number.isFinite(delta) && delta >= 0 ? delta : null;
}

function elapsedSeconds(current: string, previous: string) {
  const elapsed = (Date.parse(current) - Date.parse(previous)) / 1000;
  return Number.isFinite(elapsed) && elapsed > 0 ? elapsed : null;
}

function normalizedMaxPoints(maxPoints: number) {
  if (!Number.isFinite(maxPoints) || maxPoints <= 0) return null;
  const normalized = Math.floor(maxPoints);
  return normalized > 0 ? normalized : null;
}

export function appendMonitoringPoint(
  history: MonitoringPoint[],
  snapshot: MonitoringSnapshot,
  maxPoints = 360,
): MonitoringPoint[] {
  const limit = normalizedMaxPoints(maxPoints);
  if (limit === null) return [];

  const previous = history.at(-1);
  if (previous?.timestamp === snapshot.timestamp) return history.slice(-limit);

  const elapsed = previous
    ? elapsedSeconds(snapshot.timestamp, previous.timestamp)
    : null;
  const requestDelta = previous
    ? counterDelta(snapshot.httpRequestCount, previous.httpRequestCount)
    : null;
  const durationDelta = previous
    ? counterDelta(
        snapshot.httpRequestDurationSeconds,
        previous.httpRequestDurationSeconds,
      )
    : null;
  const errorDelta = previous
    ? counterDelta(snapshot.httpServerErrorCount, previous.httpServerErrorCount)
    : null;

  const canCalculateInterval = elapsed !== null && requestDelta !== null;
  const tps = canCalculateInterval ? requestDelta / elapsed : null;
  const hasRequests = canCalculateInterval && requestDelta > 0;

  const next: MonitoringPoint = {
    ...snapshot,
    tps,
    averageResponseMs:
      hasRequests && durationDelta !== null
        ? (durationDelta * 1000) / requestDelta
        : null,
    serverErrorRate:
      hasRequests && errorDelta !== null && errorDelta <= requestDelta
        ? (errorDelta * 100) / requestDelta
        : null,
  };

  return [...history, next].slice(-limit);
}
