package com.ino.admin.web;

import com.ino.admin.core.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

public final class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory(Clock clock) { this.clock = clock; }

    public ApiError create(ErrorCode errorCode, List<ApiError.FieldError> fieldErrors) {
        return create(errorCode.code(), errorCode.message(), fieldErrors);
    }

    public ApiError create(ErrorCode errorCode, String message, List<ApiError.FieldError> fieldErrors) {
        return create(errorCode.code(), message, fieldErrors);
    }

    public ApiError create(String code, String message, List<ApiError.FieldError> fieldErrors) {
        return new ApiError(code, message, List.copyOf(fieldErrors), MDC.get(TraceIdFilter.MDC_KEY), Instant.now(clock));
    }
}
