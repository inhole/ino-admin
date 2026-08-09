package com.ino.admin.web;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<FieldError> fieldErrors,
        String traceId,
        Instant timestamp
) {
    public record FieldError(String field, String reason) {}
}
