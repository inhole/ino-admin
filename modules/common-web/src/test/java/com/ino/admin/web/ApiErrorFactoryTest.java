package com.ino.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.core.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ApiErrorFactoryTest {
    @Test
    void createsTheExistingErrorContractWithTraceIdAndFixedTimestamp() {
        var now = Instant.parse("2026-08-30T00:00:00Z");
        MDC.put(TraceIdFilter.MDC_KEY, "test-trace-id");
        try {
            var error = new ApiErrorFactory(Clock.fixed(now, ZoneOffset.UTC))
                    .create(ErrorCode.VALIDATION_ERROR, List.of(new ApiError.FieldError("email", "INVALID_VALUE")));

            assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).isEqualTo("요청 값이 올바르지 않습니다.");
            assertThat(error.fieldErrors()).containsExactly(new ApiError.FieldError("email", "INVALID_VALUE"));
            assertThat(error.traceId()).isEqualTo("test-trace-id");
            assertThat(error.timestamp()).isEqualTo(now);
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }
}
