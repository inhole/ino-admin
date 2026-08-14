package com.ino.admin.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshToken {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    static RefreshToken issue(User user, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        var token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.user = user;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.createdAt = now;
        token.expiresAt = expiresAt;
        return token;
    }

    boolean isExpiredAt(Instant now) { return !expiresAt.isAfter(now); }
    boolean isRevoked() { return revokedAt != null; }
    void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    void replaceWith(UUID replacementId, Instant now) { revoke(now); replacedBy = replacementId; }
    UUID id() { return id; }
    User user() { return user; }
    UUID familyId() { return familyId; }
    String tokenHash() { return tokenHash; }
}
