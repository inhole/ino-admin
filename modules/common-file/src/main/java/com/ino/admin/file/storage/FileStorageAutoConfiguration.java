package com.ino.admin.file.storage;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {
    @Bean @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "local", matchIfMissing = true)
    FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(Path.of(properties.getRoot()));
    }
    @Bean @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    S3Client s3Client(FileStorageProperties properties) {
        var s3 = properties.getS3();
        var builder = S3Client.builder().region(Region.of(s3.getRegion()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccess()).build());
        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) builder.endpointOverride(URI.create(s3.getEndpoint()));
        if ((s3.getAccessKey() == null) != (s3.getSecretKey() == null))
            throw new IllegalStateException("S3 access key와 secret key는 함께 설정해야 합니다.");
        if (s3.getAccessKey() != null) builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        return builder.build();
    }
    @Bean @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    FileStorage s3FileStorage(S3Client client, FileStorageProperties properties) {
        return new S3FileStorage(client, properties.getS3().getBucket());
    }
}
