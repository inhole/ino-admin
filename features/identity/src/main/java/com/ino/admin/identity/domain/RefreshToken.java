package com.ino.admin.identity.domain;

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
public class RefreshToken {
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

    public static RefreshToken issue(User user, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        var token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.user = user;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.createdAt = now;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isExpiredAt(Instant now) { return !expiresAt.isAfter(now); }
    public boolean isRevoked() { return revokedAt != null; }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    public void replaceWith(UUID replacementId, Instant now) { revoke(now); replacedBy = replacementId; }
    public UUID id() { return id; }
    public User user() { return user; }
    public UUID familyId() { return familyId; }
    public String tokenHash() { return tokenHash; }
}
