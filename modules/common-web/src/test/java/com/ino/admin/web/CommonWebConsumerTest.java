package com.ino.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CommonWebConsumerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class))
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void independentConsumerReceivesTheCommonWebBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ApiErrorFactory.class);
            assertThat(context).hasSingleBean(TraceIdFilter.class);
        });
    }
}
