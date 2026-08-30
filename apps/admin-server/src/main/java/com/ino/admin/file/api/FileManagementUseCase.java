package com.ino.admin.file.api;

import java.time.Instant;
import java.util.UUID;

public interface FileManagementUseCase {
    StoredFile upload(UUID ownerId, UploadFile command);
    FilePage list(UUID ownerId, FileListQuery query, int page, int size);
    FileDownload download(UUID requesterId, UUID fileId);
    void delete(UUID requesterId, UUID fileId);

    enum FileSort { CREATED_AT, ORIGINAL_NAME, SIZE }
    enum SortDirection { ASC, DESC }

    record FileListQuery(String name, String contentType, Instant createdFrom, Instant createdTo,
            FileSort sort, SortDirection direction) {}
    record UploadFile(String originalName, String contentType, byte[] content) {}
    record StoredFile(UUID id, String originalName, String contentType, long size) {}
    record FileSummary(UUID id, String originalName, String contentType, long size, java.time.Instant createdAt) {}
    record FilePage(java.util.List<FileSummary> content, int page, int size, long totalElements, int totalPages) {}
    record FileDownload(String originalName, String contentType, byte[] content) {}
}
