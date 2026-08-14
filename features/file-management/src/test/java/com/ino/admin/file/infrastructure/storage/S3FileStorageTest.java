package com.ino.admin.file.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3FileStorageTest {
    @Test void usesConfiguredBucketAndOpaqueStorageKey() {
        var client = mock(S3Client.class);
        var storage = new S3FileStorage(client, "admin-files");
        storage.save("opaque-key", new byte[] { 1, 2 });
        storage.delete("opaque-key");

        var put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("admin-files");
        assertThat(put.getValue().key()).isEqualTo("opaque-key");
        verify(client).deleteObject(DeleteObjectRequest.builder().bucket("admin-files").key("opaque-key").build());
    }

    @Test void downloadsObjectBytes() {
        var client = mock(S3Client.class);
        var response = new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream("content".getBytes())));
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        var storage = new S3FileStorage(client, "admin-files");
        assertThat(storage.load("opaque-key")).isEqualTo("content".getBytes());
    }
}
