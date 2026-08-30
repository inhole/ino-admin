package com.ino.admin.auth;

import com.ino.admin.identity.application.port.AccessTokenIssuer;
import com.ino.admin.security.jwt.JwtTokenService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtTokenService jwtTokenService;

    JwtAccessTokenIssuer(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public IssuedAccessToken issue(UUID userId, String role, List<String> permissions) {
        var issued = jwtTokenService.issue(userId, role, permissions);
        return new IssuedAccessToken(issued.value(), issued.expiresInSeconds());
    }
}
