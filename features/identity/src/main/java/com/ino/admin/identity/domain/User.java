package com.ino.admin.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected User() {}

    public static User createInitialAdmin(String email, String passwordHash, String displayName, Instant now) {
        return create(email, passwordHash, displayName, UserRole.SUPER_ADMIN, now);
    }

    public static User create(String email, String passwordHash, String displayName, UserRole role, Instant now) {
        var user = new User();
        user.id = UUID.randomUUID();
        user.email = email.strip().toLowerCase(Locale.ROOT);
        user.passwordHash = passwordHash;
        user.displayName = displayName.strip();
        user.status = UserStatus.ACTIVE;
        user.role = role;
        user.failedLoginAttempts = 0;
        user.passwordChangedAt = now;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public UUID id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String displayName() { return displayName; }
    public UserStatus status() { return status; }
    public UserRole role() { return role; }
    public int failedLoginAttempts() { return failedLoginAttempts; }
    public Instant lockedAt() { return lockedAt; }
    public Instant createdAt() { return createdAt; }

    public void recordFailedLogin(int maxFailedAttempts, Instant now) {
        if (status != UserStatus.ACTIVE) return;
        failedLoginAttempts++;
        updatedAt = now;
        if (failedLoginAttempts >= maxFailedAttempts) {
            status = UserStatus.LOCKED;
            lockedAt = now;
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        if (failedLoginAttempts == 0) return;
        failedLoginAttempts = 0;
        updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        passwordHash = newPasswordHash;
        passwordChangedAt = now;
        updatedAt = now;
    }

    public void changeStatus(UserStatus newStatus, Instant now) {
        status = newStatus;
        if (newStatus == UserStatus.ACTIVE) {
            failedLoginAttempts = 0;
            lockedAt = null;
        }
        updatedAt = now;
    }
}
