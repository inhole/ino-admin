package com.ino.admin.stagedconsumer;

import com.ino.admin.audit.AuditWriter;
import java.time.Clock;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
@EnableAutoConfiguration
public class StagedConsumerApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditWriter auditWriter() {
        return command -> {};
    }
}
