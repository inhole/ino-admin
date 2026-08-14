package com.ino.admin.file.config;

import com.ino.admin.file.application.port.FileStorage;
import com.ino.admin.file.infrastructure.storage.LocalFileStorage;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfiguration {
    @Bean FileStorage fileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.getRoot()));
    }
}
