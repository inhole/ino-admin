package com.ino.admin.file.infrastructure.persistence;

import com.ino.admin.file.domain.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    Page<StoredFile> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
