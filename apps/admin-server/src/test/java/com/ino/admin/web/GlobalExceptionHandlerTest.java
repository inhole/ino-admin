package com.ino.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    @Test void mapsMultipartLimitToPayloadTooLarge() {
        var response = new GlobalExceptionHandler(Clock.systemUTC())
                .handleMaxUploadSize(new MaxUploadSizeExceededException(10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void logsUnexpectedExceptionWithTraceIdAndPreservesErrorContract(CapturedOutput output) {
        var now = Instant.parse("2026-08-23T00:00:00Z");
        MDC.put(TraceIdFilter.MDC_KEY, "unexpected-trace-id");
        try {
            var response = new GlobalExceptionHandler(Clock.fixed(now, ZoneOffset.UTC))
                    .handleUnexpected(new IllegalStateException("unexpected failure"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().message()).isEqualTo("서버 오류가 발생했습니다.");
            assertThat(response.getBody().fieldErrors()).isEmpty();
            assertThat(response.getBody().traceId()).isEqualTo("unexpected-trace-id");
            assertThat(response.getBody().timestamp()).isEqualTo(now);
            assertThat(output).contains("Unexpected server error. traceId=unexpected-trace-id")
                    .contains("java.lang.IllegalStateException: unexpected failure");
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }
}
