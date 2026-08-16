package com.ino.admin.file.infrastructure.persistence;

import com.ino.admin.file.domain.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ino.admin.file.domain.FileStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    @Query("""
            select file from StoredFile file
            where file.ownerId = :ownerId
              and file.status = :status
              and (:name is null or lower(file.originalName) like lower(concat('%', :name, '%')))
              and (:contentType is null or file.contentType = :contentType)
              and (:createdFrom is null or file.createdAt >= :createdFrom)
              and (:createdTo is null or file.createdAt < :createdTo)
            """)
    Page<StoredFile> search(@Param("ownerId") UUID ownerId, @Param("status") FileStatus status,
            @Param("name") String name, @Param("contentType") String contentType,
            @Param("createdFrom") Instant createdFrom, @Param("createdTo") Instant createdTo, Pageable pageable);
    List<StoredFile> findAllByStatusOrderByDeleteRequestedAtAsc(FileStatus status, Pageable pageable);
}
