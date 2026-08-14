package com.ino.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserLoginLockoutTest {
    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");

    @Test
    void locksActiveUserWhenFailureThresholdIsReached() {
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", NOW.minusSeconds(60));

        for (int attempt = 0; attempt < 5; attempt++) {
            user.recordFailedLogin(5, NOW.plusSeconds(attempt));
        }

        assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.failedLoginAttempts()).isEqualTo(5);
        assertThat(user.lockedAt()).isEqualTo(NOW.plusSeconds(4));
    }

    @Test
    void successfulLoginClearsPreviousFailures() {
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", NOW.minusSeconds(60));
        user.recordFailedLogin(5, NOW.minusSeconds(1));

        user.recordSuccessfulLogin(NOW);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.failedLoginAttempts()).isZero();
        assertThat(user.lockedAt()).isNull();
    }
}
