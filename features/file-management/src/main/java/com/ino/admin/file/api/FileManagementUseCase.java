package com.ino.admin.file.api;

import java.util.UUID;

public interface FileManagementUseCase {
    StoredFile upload(UUID ownerId, UploadFile command);
    FileDownload download(UUID requesterId, UUID fileId);

    record UploadFile(String originalName, String contentType, byte[] content) {}
    record StoredFile(UUID id, String originalName, String contentType, long size) {}
    record FileDownload(String originalName, String contentType, byte[] content) {}
}
