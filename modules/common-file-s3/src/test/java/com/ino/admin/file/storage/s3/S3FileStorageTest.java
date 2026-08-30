package com.ino.admin.file.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3FileStorageTest {
    @Test
    void savesAndDeletesUsingConfiguredBucketAndOpaqueKey() {
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

    @Test
    void loadsObjectBytes() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream("content".getBytes()))));
        assertThat(new S3FileStorage(client, "admin-files").load("opaque-key")).isEqualTo("content".getBytes());
    }
}
