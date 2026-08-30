package com.ino.admin.file.infrastructure.persistence;

import com.ino.admin.file.domain.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ino.admin.file.domain.FileStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID>, JpaSpecificationExecutor<StoredFile> {
    List<StoredFile> findAllByStatusOrderByDeleteRequestedAtAsc(FileStatus status, Pageable pageable);
}
