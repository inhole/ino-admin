package com.ino.admin.file.application;

import com.ino.admin.file.storage.FileStorage;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FileDeletionCoordinator {
    private static final Logger log = LoggerFactory.getLogger(FileDeletionCoordinator.class);
    private final FileDeletionStateService state;
    private final FileStorage storage;

    public FileDeletionCoordinator(FileDeletionStateService state, FileStorage storage) {
        this.state = state; this.storage = storage;
    }

    public void delete(UUID requesterId, UUID fileId) {
        deleteObject(state.markDeleting(requesterId, fileId), true);
    }

    @Scheduled(fixedDelayString = "${app.file-storage.cleanup-delay:1m}", initialDelayString = "${app.file-storage.cleanup-initial-delay:1m}")
    public void retryPendingDeletes() {
        state.pending(100).forEach(target -> deleteObject(target, false));
    }

    private void deleteObject(FileDeletionStateService.DeletionTarget target, boolean propagateFailure) {
        try {
            storage.delete(target.storageKey());
            state.removeMetadata(target.fileId());
        } catch (RuntimeException exception) {
            log.warn("File object deletion failed; queued for retry. fileId={}", target.fileId());
            if (propagateFailure) throw exception;
        }
    }
}
