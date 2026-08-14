package com.ino.admin.config;

import com.ino.admin.file.config.FileStorageProperties;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {
    private static final DataSize MULTIPART_OVERHEAD = DataSize.ofMegabytes(1);

    @Bean
    MultipartConfigElement multipartConfigElement(FileStorageProperties properties) {
        var factory = new MultipartConfigFactory();
        factory.setMaxFileSize(properties.getMaxSize());
        factory.setMaxRequestSize(DataSize.ofBytes(Math.addExact(
                properties.getMaxSize().toBytes(), MULTIPART_OVERHEAD.toBytes())));
        return factory.createMultipartConfig();
    }
}
