package com.ino.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.spring.modules.audit.AuditActor;
import com.ino.spring.modules.audit.AuditCommand;
import com.ino.spring.modules.audit.AuditResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditLogTest {
    @Test
    void persistsOnlyAllowlistedActorAndRequestAttributes() {
        var command = new AuditCommand(new AuditActor(null, Map.of(
                AuditAttributeKeys.LOGIN_EMAIL, "admin@example.com",
                AuditAttributeKeys.LOGIN_DISPLAY_NAME, "관리자",
                AuditAttributeKeys.LOGIN_ROLE, "SUPER_ADMIN",
                "password", "secret")),
                "AUTH_LOGIN", "/api/v1/auth/login", AuditResult.SUCCESS, 200, "trace-1", Map.of(
                AuditAttributeKeys.IP_ADDRESS, "127.0.0.1",
                AuditAttributeKeys.USER_AGENT, "browser",
                "authorization", "Bearer secret-token"));

        var log = AuditLog.create(command, Instant.parse("2026-08-24T00:00:00Z"));

        assertThat(log.loginEmail()).isEqualTo("admin@example.com");
        assertThat(log.loginDisplayName()).isEqualTo("관리자");
        assertThat(log.loginRole()).isEqualTo("SUPER_ADMIN");
        assertThat(log.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(log.userAgent()).isEqualTo("browser");
        assertThat(Arrays.stream(AuditLog.class.getDeclaredFields()).map(field -> field.getName()))
                .doesNotContain("password", "authorization");
    }
}
