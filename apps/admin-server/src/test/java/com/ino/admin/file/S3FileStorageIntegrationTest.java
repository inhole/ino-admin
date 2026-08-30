package com.ino.admin.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ino.admin.file.storage.FileStorage;
import com.ino.admin.file.storage.s3.S3FileStorageProperties;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Tag("integration")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "APP_FILE_STORAGE_TYPE", matches = "s3")
class S3FileStorageIntegrationTest {
    @Autowired S3Client client;
    @Autowired FileStorage storage;
    @Autowired S3FileStorageProperties properties;

    @Test void storesLoadsAndDeletesAgainstMinio() {
        var bucket = properties.getBucket();
        createBucketIfMissing(bucket);
        var key = "contract/" + UUID.randomUUID();
        var content = "minio-contract".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        storage.save(key, content);
        assertThat(storage.load(key)).isEqualTo(content);
        storage.delete(key);
        assertThatThrownBy(() -> client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()))
                .isInstanceOf(S3Exception.class)
                .satisfies(exception -> assertThat(((S3Exception) exception).statusCode()).isEqualTo(404));
    }

    private void createBucketIfMissing(String bucket) {
        try { client.headBucket(builder -> builder.bucket(bucket)); }
        catch (S3Exception exception) {
            if (exception.statusCode() != 404) throw exception;
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }
}
