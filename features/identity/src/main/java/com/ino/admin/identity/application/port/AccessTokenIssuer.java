package com.ino.admin.identity.application.port;

import java.util.UUID;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(UUID userId, String role);

    record IssuedAccessToken(String value, long expiresInSeconds) {}
}
