package com.ino.admin.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtSecurityConsumerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtSecurityAutoConfiguration.class))
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues(
                    "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                    "app.jwt.issuer=test-issuer",
                    "app.jwt.audience=test-audience",
                    "app.jwt.access-token-ttl=5m");

    @Test
    void independentConsumerReceivesJwtExtensionPoints() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtEncoder.class);
            assertThat(context).hasSingleBean(JwtDecoder.class);
            assertThat(context).hasSingleBean(JwtTokenService.class);
            assertThat(context).hasSingleBean(JwtPermissionAuthenticationConverter.class);
        });
    }

    @Test
    void rejectsSecretsShorterThan256Bits() {
        contextRunner.withPropertyValues("app.jwt.secret=c2hvcnQ=").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void preservesConsumerProvidedPermissionConverter() {
        var override = new JwtPermissionAuthenticationConverter();
        contextRunner.withBean(JwtPermissionAuthenticationConverter.class, () -> override).run(context ->
                assertThat(context.getBean(JwtPermissionAuthenticationConverter.class)).isSameAs(override));
    }
}
