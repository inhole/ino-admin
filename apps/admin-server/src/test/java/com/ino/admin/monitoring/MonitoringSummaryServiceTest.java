package com.ino.admin.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MonitoringSummaryServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Test
    void summarizesStandardMicrometerGaugesAndAggregatesHttpTimers() {
        var registry = new SimpleMeterRegistry();
        registerGauge(registry, "system.cpu.usage", 0.42d);
        registerGauge(registry, "process.cpu.usage", 0.21d);
        registerGauge(registry, "jvm.memory.used", 512d, "area", "heap", "id", "G1 Eden Space");
        registerGauge(registry, "jvm.memory.used", 512d, "area", "heap", "id", "G1 Old Gen");
        registerGauge(registry, "jvm.memory.max", 2048d, "area", "heap", "id", "G1 Eden Space");
        registerGauge(registry, "jvm.memory.max", 2048d, "area", "heap", "id", "G1 Old Gen");
        TimeGauge.builder("process.uptime", () -> 3_600_000L, TimeUnit.MILLISECONDS).register(registry);
        registerGauge(registry, "jvm.threads.live", 12d);
        registerGauge(registry, "jvm.threads.peak", 18d);
        recordRequest(registry, "200", Duration.ofMillis(250));
        recordRequest(registry, "200", Duration.ofMillis(250));
        recordRequest(registry, "503", Duration.ofMillis(125));

        var summary = new MonitoringSummaryService(registry, Clock.fixed(NOW, ZoneOffset.UTC)).getSummary();

        assertThat(summary).isEqualTo(new MonitoringSummary(
                NOW, 0.42d, 0.21d, 1024d, 4096d, 3600d, 12d, 18d, 3L, 0.625d, 1L));
    }

    @Test
    void returnsNullForMissingMeters() {
        var registry = new SimpleMeterRegistry();
        var summary = new MonitoringSummaryService(registry, Clock.fixed(NOW, ZoneOffset.UTC)).getSummary();

        assertThat(summary).isEqualTo(new MonitoringSummary(
                NOW, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    void returnsNullForNonFiniteGaugeValues() {
        var registry = new SimpleMeterRegistry();
        registerGauge(registry, "system.cpu.usage", Double.NaN);

        var summary = new MonitoringSummaryService(registry, Clock.fixed(NOW, ZoneOffset.UTC)).getSummary();

        assertThat(summary.systemCpuUsage()).isNull();
    }

    private static void registerGauge(SimpleMeterRegistry registry, String name, double value, String... tags) {
        Gauge.builder(name, () -> value).tags(tags).register(registry);
    }

    private static void recordRequest(SimpleMeterRegistry registry, String status, Duration duration) {
        Timer.builder("http.server.requests")
                .tag("status", status)
                .tag("uri", "/api/v1/users")
                .register(registry)
                .record(duration);
    }
}
