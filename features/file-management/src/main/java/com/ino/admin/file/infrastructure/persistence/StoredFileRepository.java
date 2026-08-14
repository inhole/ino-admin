package com.ino.admin.file.infrastructure.persistence;

import com.ino.admin.file.domain.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ino.admin.file.domain.FileStatus;
import java.util.List;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    Page<StoredFile> findAllByOwnerIdAndStatus(UUID ownerId, FileStatus status, Pageable pageable);
    List<StoredFile> findAllByStatusOrderByDeleteRequestedAtAsc(FileStatus status, Pageable pageable);
}
