package com.ino.admin.identity.application;

import com.ino.admin.identity.api.InvalidRefreshTokenException;
import com.ino.admin.identity.api.RefreshTokenUseCase;
import com.ino.admin.identity.application.port.AccessTokenIssuer;
import com.ino.admin.identity.domain.RefreshToken;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.RefreshTokenRepository;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

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
public class RefreshTokenService implements RefreshTokenUseCase {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Duration ttl;
    private final RolePermissionService rolePermissionService;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository repository, AccessTokenIssuer accessTokenIssuer, Clock clock,
            @Value("${ino.spring.modules.jwt.refresh-token-ttl:30d}") Duration ttl, RolePermissionService rolePermissionService,
            UserRepository userRepository) {
        if (ttl.isZero() || ttl.isNegative()) throw new IllegalStateException("Refresh token TTL은 0보다 커야 합니다.");
        this.repository = repository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
        this.ttl = ttl;
        this.rolePermissionService = rolePermissionService;
        this.userRepository = userRepository;
    }

    @Transactional
    IssuedRefreshToken issue(User user) {
        return issue(user, UUID.randomUUID(), Instant.now(clock));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    @Override
    public RefreshResult rotate(String rawToken) {
        var now = Instant.now(clock);
        var tokenHash = hash(rawToken);
        var userId = repository.findUserIdByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);
        var familyId = repository.findFamilyIdByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);
        var user = userRepository.findByIdForUpdate(userId).orElseThrow(InvalidRefreshTokenException::new);
        var role = rolePermissionService.findTokenPermissionsForUpdate(user.role());
        var current = repository.findAllByFamilyId(familyId).stream()
                .filter(token -> token.tokenHash().equals(tokenHash))
                .findFirst().orElseThrow(InvalidRefreshTokenException::new);
        if (current.isRevoked()) {
            revokeFamily(current.familyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now) || user.status() != UserStatus.ACTIVE || !role.enabled()) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }
        var replacement = newToken(user, current.familyId(), now);
        repository.save(replacement.entity());
        current.replaceWith(replacement.entity().id(), now);
        var access = accessTokenIssuer.issue(user.id(), user.role(), role.permissions());
        return new RefreshResult(access.value(), access.expiresInSeconds(), replacement.rawToken());
    }

    @Transactional
    @Override
    public void logout(String rawToken) {
        repository.findFamilyIdByTokenHash(hash(rawToken))
                .ifPresent(familyId -> revokeFamily(familyId, Instant.now(clock)));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        var now = Instant.now(clock);
        repository.findAllByUser_Id(userId).forEach(token -> token.revoke(now));
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
}
