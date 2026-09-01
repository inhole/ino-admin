package com.ino.admin.stagedconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.security.jwt.JwtSecurityAutoConfiguration;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StagedConsumerMissingConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtSecurityAutoConfiguration.class))
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void explainsThatTheJwtSecretIsRequired() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("APP_JWT_SECRET 설정이 필요합니다.");
        });
    }
}
