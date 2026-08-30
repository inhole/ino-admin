package com.ino.admin.consumer;

import com.ino.admin.audit.AuditWriter;
import java.time.Clock;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
@EnableAutoConfiguration
public class CommonModulesConsumerApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditWriter auditWriter() {
        return command -> {};
    }
}
