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

function counterDelta(current: number | null, previous: number | null) {
  if (
    current === null ||
    previous === null ||
    !Number.isFinite(current) ||
    !Number.isFinite(previous)
  ) {
    return null;
  }

  const delta = current - previous;
  return delta >= 0 ? delta : null;
}

function elapsedSeconds(current: string, previous: string) {
  const elapsed = (Date.parse(current) - Date.parse(previous)) / 1000;
  return Number.isFinite(elapsed) && elapsed > 0 ? elapsed : null;
}

export function appendMonitoringPoint(
  history: MonitoringPoint[],
  snapshot: MonitoringSnapshot,
  maxPoints = 360,
): MonitoringPoint[] {
  const previous = history.at(-1);
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
      hasRequests && errorDelta !== null
        ? (errorDelta * 100) / requestDelta
        : null,
  };

  return [...history, next].slice(-maxPoints);
}
