package com.ino.admin.file.storage.s3;

import com.ino.admin.file.storage.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public final class S3FileStorage implements FileStorage {
    private final S3Client client;
    private final String bucket;

    public S3FileStorage(S3Client client, String bucket) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("S3 bucket은 필수입니다.");
        this.client = client;
        this.bucket = bucket;
    }

    public void save(String key, byte[] content) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(content));
    }

    public byte[] load(String key) {
        try (var response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return response.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("S3 파일을 읽을 수 없습니다.", exception);
        }
    }

    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
