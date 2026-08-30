package com.ino.admin.file.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ino.admin.file.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

class S3FileStorageConsumerTest {
    @Test
    void s3ConsumerGetsS3Storage() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3FileStorageAutoConfiguration.class))
                .withBean(S3Client.class, () -> mock(S3Client.class))
                .withPropertyValues("app.file-storage.type=s3", "app.file-storage.s3.bucket=admin-files")
                .run(context -> assertThat(context).hasSingleBean(FileStorage.class)
                        .getBean(FileStorage.class).isInstanceOf(S3FileStorage.class));
    }

    @Test
    void preservesConsumerProvidedStorage() {
        FileStorage override = new FileStorage() {
            public void save(String key, byte[] content) {}
            public byte[] load(String key) { return new byte[0]; }
            public void delete(String key) {}
        };
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3FileStorageAutoConfiguration.class))
                .withBean(S3Client.class, () -> mock(S3Client.class))
                .withBean(FileStorage.class, () -> override)
                .withPropertyValues("app.file-storage.type=s3")
                .run(context -> assertThat(context.getBean(FileStorage.class)).isSameAs(override));
    }
}
