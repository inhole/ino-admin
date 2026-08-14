package com.ino.admin.file.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stored_files")
public class StoredFile {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "storage_key", nullable = false, unique = true, length = 100) private String storageKey;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(nullable = false) private long size;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected StoredFile() {}

    public static StoredFile create(UUID ownerId, String originalName, String storageKey, String contentType, long size, Instant now) {
        var file = new StoredFile();
        file.id = UUID.randomUUID(); file.ownerId = ownerId; file.originalName = originalName;
        file.storageKey = storageKey; file.contentType = contentType; file.size = size; file.createdAt = now;
        return file;
    }

    public UUID id() { return id; }
    public UUID ownerId() { return ownerId; }
    public String originalName() { return originalName; }
    public String storageKey() { return storageKey; }
    public String contentType() { return contentType; }
    public long size() { return size; }
}
