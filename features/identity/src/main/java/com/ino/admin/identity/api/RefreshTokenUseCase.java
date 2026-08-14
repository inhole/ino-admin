package com.ino.admin.identity.api;

public interface RefreshTokenUseCase {
    RefreshResult rotate(String rawToken);
    void logout(String rawToken);

    record RefreshResult(String accessToken, long expiresInSeconds, String refreshToken) {}
}
