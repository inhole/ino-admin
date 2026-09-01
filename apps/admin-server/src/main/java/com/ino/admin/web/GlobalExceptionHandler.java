package com.ino.admin.web;

import com.ino.spring.modules.core.BusinessException;
import com.ino.spring.modules.web.ApiError;
import com.ino.spring.modules.web.ApiErrorFactory;
import com.ino.spring.modules.web.TraceIdFilter;
import com.ino.admin.error.ErrorCode;
import com.ino.admin.identity.api.AuthenticationFailedException;
import com.ino.admin.identity.api.InvalidRefreshTokenException;
import com.ino.admin.file.api.FileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ApiErrorFactory apiErrorFactory;

    public GlobalExceptionHandler(Clock clock) {
        this.apiErrorFactory = new ApiErrorFactory(clock);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), "INVALID_VALUE"))
                .toList();
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException exception) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldError(result.getMethodParameter().getParameterName(), "INVALID_VALUE")))
                .toList();
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, errors);
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
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(FileNotFoundException.class)
    ResponseEntity<ApiError> handleFileNotFound(FileNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, ErrorCode.FILE_NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.FILE_TOO_LARGE, List.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiError> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return response(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiError> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return response(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN, exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unexpected server error. traceId={}", MDC.get(TraceIdFilter.MDC_KEY), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, List<ApiError.FieldError> errors) {
        var body = apiErrorFactory.create(code, message, errors);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ErrorCode errorCode, List<ApiError.FieldError> errors) {
        return response(status, errorCode.code(), errorCode.message(), errors);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ErrorCode errorCode, String message,
            List<ApiError.FieldError> errors) {
        return response(status, errorCode.code(), message, errors);
    }
}
