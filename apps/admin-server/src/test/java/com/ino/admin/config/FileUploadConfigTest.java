package com.ino.admin.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.file.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class FileUploadConfigTest {
    @Test void alignsServletAndDomainFileSizeLimits() {
        var properties = new FileStorageProperties();
        properties.setMaxSize(DataSize.ofMegabytes(25));

        var multipart = new FileUploadConfig().multipartConfigElement(properties);

        assertThat(multipart.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(25).toBytes());
        assertThat(multipart.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(26).toBytes());
    }
}
