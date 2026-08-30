package com.ino.admin.web;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonWebAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    TraceIdFilter traceIdFilter() { return new TraceIdFilter(); }

    @Bean @ConditionalOnMissingBean
    ApiErrorFactory apiErrorFactory(Clock clock) { return new ApiErrorFactory(clock); }
}
