package com.ino.admin.file.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ino.admin.file.api.FileNotFoundException;
import com.ino.admin.file.domain.StoredFile;
import com.ino.admin.file.infrastructure.persistence.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FileDeletionStateServiceTest {
    private final StoredFileRepository repository = Mockito.mock(StoredFileRepository.class);
    private final FileDeletionStateService service = new FileDeletionStateService(repository,
            Clock.fixed(Instant.parse("2026-08-23T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void hidesAnotherOwnersFileWhenDeleting() {
        var requesterId = UUID.randomUUID();
        var file = StoredFile.create(UUID.randomUUID(), "report.pdf", "key", "application/pdf", 1,
                Instant.parse("2026-08-22T00:00:00Z"));
        when(repository.findById(file.id())).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.markDeleting(requesterId, file.id()))
                .isInstanceOf(FileNotFoundException.class);

        verify(repository).findById(file.id());
        verifyNoMoreInteractions(repository);
    }
}
