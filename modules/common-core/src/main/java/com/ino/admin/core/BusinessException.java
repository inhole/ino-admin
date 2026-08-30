package com.ino.admin.core;

public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorDescriptor error) {
        this(error.code(), error.message());
    }

    public BusinessException(ErrorDescriptor error, String message) {
        this(error.code(), message);
    }

    public String code() {
        return code;
    }
}
