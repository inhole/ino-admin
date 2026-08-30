package com.ino.admin.file.storage.s3;

import com.ino.admin.file.storage.FileStorage;
import java.net.URI;
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
@EnableConfigurationProperties(S3FileStorageProperties.class)
public class S3FileStorageAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    S3Client s3Client(S3FileStorageProperties properties) {
        var builder = S3Client.builder().region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess()).build());
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        if ((properties.getAccessKey() == null) != (properties.getSecretKey() == null)) {
            throw new IllegalStateException("S3 access key와 secret key는 함께 설정해야 합니다.");
        }
        if (properties.getAccessKey() != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    @ConditionalOnProperty(prefix = "app.file-storage", name = "type", havingValue = "s3")
    FileStorage s3FileStorage(S3Client client, S3FileStorageProperties properties) {
        return new S3FileStorage(client, properties.getBucket());
    }
}
