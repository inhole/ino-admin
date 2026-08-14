package com.ino.admin.web;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.AuthenticationFailedException;
import com.ino.admin.identity.api.InvalidRefreshTokenException;
import com.ino.admin.file.api.FileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), "INVALID_VALUE"))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException exception) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldError(result.getMethodParameter().getParameterName(), "INVALID_VALUE")))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        var errors = exception.getConstraintViolations().stream()
                .map(violation -> {
                    var path = violation.getPropertyPath().toString();
                    var separator = path.lastIndexOf('.');
                    var field = separator < 0 ? path : path.substring(separator + 1);
                    return new ApiError.FieldError(field, "INVALID_VALUE");
                })
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", errors);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(FileNotFoundException.class)
    ResponseEntity<ApiError> handleFileNotFound(FileNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiError> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.", List.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, List<ApiError.FieldError> errors) {
        var body = new ApiError(code, message, errors, MDC.get(TraceIdFilter.MDC_KEY), Instant.now(clock));
        return ResponseEntity.status(status).body(body);
    }
}
