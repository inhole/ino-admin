package com.ino.admin.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtTokenServiceTest {
    @Test
    void issuesTheExistingSubjectRolePermissionAndLifetimeClaims() {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var userId = UUID.randomUUID();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JwtSecurityAutoConfiguration.class))
                .withBean(Clock.class, () -> Clock.fixed(now, ZoneOffset.UTC))
                .withPropertyValues(
                        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                        "app.jwt.issuer=test-issuer", "app.jwt.audience=test-audience",
                        "app.jwt.access-token-ttl=15m")
                .run(context -> {
                    var issued = context.getBean(JwtTokenService.class)
                            .issue(userId, "ADMIN", List.of("user:read", "file:read"));
                    var jwt = context.getBean(JwtDecoder.class).decode(issued.value());

                    assertThat(jwt.getSubject()).isEqualTo(userId.toString());
                    assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
                    assertThat(jwt.getClaimAsStringList("permissions")).containsExactly("user:read", "file:read");
                    assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
                    assertThat(jwt.getAudience()).containsExactly("test-audience");
                    assertThat(jwt.getIssuedAt()).isEqualTo(now);
                    assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(900));
                    assertThat(issued.expiresInSeconds()).isEqualTo(900);
                });
    }
}
