package com.ino.admin.monitoring;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class MonitoringSummaryService {
    private static final String HTTP_REQUESTS = "http.server.requests";

    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public MonitoringSummaryService(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public MonitoringSummary getSummary() {
        var httpTimers = httpRequestTimers();
        return new MonitoringSummary(
                clock.instant(),
                gaugeValue("system.cpu.usage"),
                gaugeValue("process.cpu.usage"),
                gaugeValue("jvm.memory.used", "area", "heap"),
                gaugeValue("jvm.memory.max", "area", "heap"),
                gaugeValue("process.uptime"),
                gaugeValue("jvm.threads.live"),
                gaugeValue("jvm.threads.peak"),
                httpTimers.isEmpty() ? null : httpTimers.stream().mapToLong(Timer::count).sum(),
                httpTimers.isEmpty() ? null : finiteOrNull(httpTimers.stream()
                        .mapToDouble(timer -> timer.totalTime(TimeUnit.SECONDS))
                        .sum()),
                httpTimers.isEmpty() ? null : httpTimers.stream()
                        .filter(timer -> isServerError(timer.getId()))
                        .mapToLong(Timer::count)
                        .sum());
    }

    private List<Timer> httpRequestTimers() {
        return meterRegistry.getMeters().stream()
                .filter(meter -> HTTP_REQUESTS.equals(meter.getId().getName()))
                .filter(Timer.class::isInstance)
                .map(Timer.class::cast)
                .toList();
    }

    private Double gaugeValue(String name, String... tags) {
        var values = meterRegistry.getMeters().stream()
                .filter(meter -> name.equals(meter.getId().getName()))
                .filter(meter -> hasTags(meter.getId(), tags))
                .filter(io.micrometer.core.instrument.Gauge.class::isInstance)
                .map(io.micrometer.core.instrument.Gauge.class::cast)
                .mapToDouble(io.micrometer.core.instrument.Gauge::value)
                .toArray();
        if (values.length == 0) {
            return null;
        }
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return null;
            }
        }
        return finiteOrNull(java.util.Arrays.stream(values).sum());
    }

    private boolean hasTags(Meter.Id meterId, String... tags) {
        for (int index = 0; index < tags.length; index += 2) {
            if (!tags[index + 1].equals(meterId.getTag(tags[index]))) {
                return false;
            }
        }
        return true;
    }

    private boolean isServerError(Meter.Id meterId) {
        String status = meterId.getTag("status");
        return status != null && status.startsWith("5");
    }

    private Double finiteOrNull(double value) {
        return Double.isFinite(value) ? value : null;
    }
}
