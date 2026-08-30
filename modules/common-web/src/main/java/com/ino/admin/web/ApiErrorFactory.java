package com.ino.admin.web;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

public final class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory(Clock clock) { this.clock = clock; }

    public ApiError create(String code, String message, List<ApiError.FieldError> fieldErrors) {
        return new ApiError(code, message, List.copyOf(fieldErrors), MDC.get(TraceIdFilter.MDC_KEY), Instant.now(clock));
    }
}
