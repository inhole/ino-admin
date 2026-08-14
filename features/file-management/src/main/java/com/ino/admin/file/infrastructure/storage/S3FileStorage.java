package com.ino.admin.file.infrastructure.storage;

import com.ino.admin.file.application.port.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3FileStorage implements FileStorage {
    private final S3Client client;
    private final String bucket;

    public S3FileStorage(S3Client client, String bucket) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("S3 bucket은 필수입니다.");
        this.client = client;
        this.bucket = bucket;
    }

    @Override public void save(String storageKey, byte[] content) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(storageKey).build(), RequestBody.fromBytes(content));
    }

    @Override public byte[] load(String storageKey) {
        try (var response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())) {
            return response.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("S3 파일을 읽을 수 없습니다.", exception);
        }
    }

    @Override public void delete(String storageKey) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }
}
