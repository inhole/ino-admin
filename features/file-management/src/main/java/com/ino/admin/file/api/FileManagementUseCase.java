package com.ino.admin.file.api;

import java.util.UUID;

public interface FileManagementUseCase {
    StoredFile upload(UUID ownerId, UploadFile command);
    FilePage list(UUID ownerId, int page, int size);
    FileDownload download(UUID requesterId, UUID fileId);
    void delete(UUID requesterId, UUID fileId);

    record UploadFile(String originalName, String contentType, byte[] content) {}
    record StoredFile(UUID id, String originalName, String contentType, long size) {}
    record FileSummary(UUID id, String originalName, String contentType, long size, java.time.Instant createdAt) {}
    record FilePage(java.util.List<FileSummary> content, int page, int size, long totalElements, int totalPages) {}
    record FileDownload(String originalName, String contentType, byte[] content) {}
}
