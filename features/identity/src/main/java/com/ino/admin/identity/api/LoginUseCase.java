package com.ino.admin.identity.api;

import java.util.List;
import java.util.UUID;

public interface LoginUseCase {
    LoginResult login(String email, String password);
    CurrentUser currentUser(UUID userId);

    record LoginResult(String accessToken, long expiresInSeconds, String refreshToken) {}
    record CurrentUser(UUID id, String email, String displayName, String status, String role, List<String> permissions) {}
}
