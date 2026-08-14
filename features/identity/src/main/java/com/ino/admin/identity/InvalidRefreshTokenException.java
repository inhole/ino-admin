package com.ino.admin.identity;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token이 유효하지 않습니다.");
    }
}
