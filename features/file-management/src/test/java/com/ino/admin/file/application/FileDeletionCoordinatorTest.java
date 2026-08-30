package com.ino.admin.file.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.file.application.FileDeletionStateService.DeletionTarget;
import com.ino.admin.file.storage.FileStorage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FileDeletionCoordinatorTest {
    @Mock FileDeletionStateService state;
    @Mock FileStorage storage;
    private FileDeletionCoordinator coordinator;

    @BeforeEach void setUp() { MockitoAnnotations.openMocks(this); coordinator = new FileDeletionCoordinator(state, storage); }

    @Test void removesMetadataAfterObjectDeletion() {
        var ownerId = UUID.randomUUID(); var fileId = UUID.randomUUID(); var target = new DeletionTarget(fileId, "key");
        when(state.markDeleting(ownerId, fileId)).thenReturn(target);
        coordinator.delete(ownerId, fileId);
        verify(storage).delete("key");
        verify(state).removeMetadata(fileId);
    }

    @Test void leavesDeletingMetadataWhenObjectDeletionFails() {
        var ownerId = UUID.randomUUID(); var fileId = UUID.randomUUID(); var target = new DeletionTarget(fileId, "key");
        when(state.markDeleting(ownerId, fileId)).thenReturn(target);
        Mockito.doThrow(new IllegalStateException("storage unavailable")).when(storage).delete("key");
        assertThatThrownBy(() -> coordinator.delete(ownerId, fileId)).isInstanceOf(IllegalStateException.class);
        verify(state, Mockito.never()).removeMetadata(fileId);
    }

    @Test void retriesPendingDeletionWithoutStoppingBatch() {
        var failed = new DeletionTarget(UUID.randomUUID(), "failed"); var recovered = new DeletionTarget(UUID.randomUUID(), "recovered");
        when(state.pending(100)).thenReturn(List.of(failed, recovered));
        Mockito.doThrow(new IllegalStateException("temporary")).when(storage).delete("failed");
        coordinator.retryPendingDeletes();
        verify(storage).delete("recovered");
        verify(state).removeMetadata(recovered.fileId());
    }
}
