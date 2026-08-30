package com.ino.admin.file.storage;

import java.nio.file.Path;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {
    @Bean @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "local", matchIfMissing = true)
    FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.getRoot()));
    }
}
