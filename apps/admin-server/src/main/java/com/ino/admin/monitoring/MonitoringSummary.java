package com.ino.admin.monitoring;

import java.time.Instant;

public record MonitoringSummary(
        Instant timestamp,
        Double systemCpuUsage,
        Double processCpuUsage,
        Double heapUsedBytes,
        Double heapMaxBytes,
        Double processUptimeSeconds,
        Double liveThreads,
        Double peakThreads,
        Long httpRequestCount,
        Double httpRequestDurationSeconds,
        Long httpServerErrorCount
) {}
