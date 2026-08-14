package com.ino.admin.file.application;

import com.ino.admin.file.api.FileNotFoundException;
import com.ino.admin.file.domain.FileStatus;
import com.ino.admin.file.infrastructure.persistence.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileDeletionStateService {
    private final StoredFileRepository repository;
    private final Clock clock;

    public FileDeletionStateService(StoredFileRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }

    @Transactional
    public DeletionTarget markDeleting(UUID requesterId, UUID fileId) {
        var file = repository.findById(fileId).filter(found -> found.ownerId().equals(requesterId))
                .orElseThrow(FileNotFoundException::new);
        file.markDeleting(Instant.now(clock));
        return new DeletionTarget(file.id(), file.storageKey());
    }

    @Transactional(readOnly = true)
    public List<DeletionTarget> pending(int limit) {
        return repository.findAllByStatusOrderByDeleteRequestedAtAsc(FileStatus.DELETING, PageRequest.of(0, limit))
                .stream().map(file -> new DeletionTarget(file.id(), file.storageKey())).toList();
    }

    @Transactional
    public void removeMetadata(UUID fileId) { repository.deleteById(fileId); }

    public record DeletionTarget(UUID fileId, String storageKey) {}
}
