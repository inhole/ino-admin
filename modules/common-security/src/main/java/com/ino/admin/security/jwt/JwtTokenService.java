package com.ino.admin.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public final class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtSecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(UUID subject, String role, List<String> permissions) {
        var issuedAt = Instant.now(clock);
        var claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject(subject.toString()).claim("role", role).claim("permissions", List.copyOf(permissions))
                .id(UUID.randomUUID().toString()).issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getAccessTokenTtl())).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, properties.getAccessTokenTtl().toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {}
}
