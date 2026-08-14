package com.ino.admin.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Duration ttl;

    RefreshTokenService(RefreshTokenRepository repository, AccessTokenIssuer accessTokenIssuer, Clock clock,
            @Value("${app.jwt.refresh-token-ttl:30d}") Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) throw new IllegalStateException("Refresh token TTL은 0보다 커야 합니다.");
        this.repository = repository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Transactional
    IssuedRefreshToken issue(User user) {
        return issue(user, UUID.randomUUID(), Instant.now(clock));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshResult rotate(String rawToken) {
        var now = Instant.now(clock);
        var current = repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (current.isRevoked()) {
            revokeFamily(current.familyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now) || current.user().status() != UserStatus.ACTIVE) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }
        var replacement = newToken(current.user(), current.familyId(), now);
        repository.save(replacement.entity());
        current.replaceWith(replacement.entity().id(), now);
        var access = accessTokenIssuer.issue(current.user().id());
        return new RefreshResult(access.value(), access.expiresInSeconds(), replacement.rawToken());
    }

    @Transactional
    public void logout(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> revokeFamily(token.familyId(), Instant.now(clock)));
    }

    private IssuedRefreshToken issue(User user, UUID familyId, Instant now) {
        var issued = newToken(user, familyId, now);
        repository.save(issued.entity());
        return issued;
    }

    private IssuedRefreshToken newToken(User user, UUID familyId, Instant now) {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        var raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedRefreshToken(raw, RefreshToken.issue(user, hash(raw), familyId, now, now.plus(ttl)));
    }

    private void revokeFamily(UUID familyId, Instant now) {
        repository.findAllByFamilyId(familyId).forEach(token -> token.revoke(now));
    }

    private String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidRefreshTokenException();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    record IssuedRefreshToken(String rawToken, RefreshToken entity) {}
    public record RefreshResult(String accessToken, long expiresInSeconds, String refreshToken) {}
}
