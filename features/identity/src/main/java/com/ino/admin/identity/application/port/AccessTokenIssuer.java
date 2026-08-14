package com.ino.admin.identity.application.port;

import java.util.List;
import java.util.UUID;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(UUID userId, String role, List<String> permissions);

    record IssuedAccessToken(String value, long expiresInSeconds) {}
}
