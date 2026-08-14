package com.ino.admin.file.config;

import com.ino.admin.file.application.port.FileStorage;
import com.ino.admin.file.infrastructure.storage.LocalFileStorage;
import com.ino.admin.file.infrastructure.storage.S3FileStorage;
import java.nio.file.Path;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "local", matchIfMissing = true)
    FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.getRoot()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    software.amazon.awssdk.services.s3.S3Client s3Client(FileStorageProperties properties) {
        var s3 = properties.getS3();
        var builder = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(s3.getRegion()))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.isPathStyleAccess()).build());
        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) builder.endpointOverride(URI.create(s3.getEndpoint()));
        if ((s3.getAccessKey() == null) != (s3.getSecretKey() == null))
            throw new IllegalStateException("S3 access key와 secret key는 함께 설정해야 합니다.");
        if (s3.getAccessKey() != null && s3.getSecretKey() != null) builder.credentialsProvider(
                software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    FileStorage s3FileStorage(software.amazon.awssdk.services.s3.S3Client client, FileStorageProperties properties) {
        return new S3FileStorage(client, properties.getS3().getBucket());
    }
}
