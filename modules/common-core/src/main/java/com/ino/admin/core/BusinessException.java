package com.ino.admin.core;

public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.code(), message);
    }

    public String code() {
        return code;
    }
}
