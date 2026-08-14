package com.ino.admin.file.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.file.api.FileNotFoundException;
import com.ino.admin.file.api.InvalidFileException;
import com.ino.admin.file.application.port.FileStorage;
import com.ino.admin.file.config.FileStorageProperties;
import com.ino.admin.file.domain.StoredFile;
import com.ino.admin.file.infrastructure.persistence.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FileManagementServiceTest {
    @Mock StoredFileRepository repository;
    @Mock FileStorage storage;
    private FileManagementService service;
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        var properties = new FileStorageProperties();
        service = new FileManagementService(repository, storage, properties,
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test void storesSafeMetadataAndContent() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.upload(ownerId, new com.ino.admin.file.api.FileManagementUseCase.UploadFile(
                "../report.pdf", "application/pdf", "%PDF-test".getBytes()));
        assertThat(result.originalName()).isEqualTo("report.pdf");
        verify(storage).save(anyString(), any());
    }

    @Test void rejectsEmptyAndMismatchedFiles() {
        assertThatThrownBy(() -> service.upload(ownerId,
                new com.ino.admin.file.api.FileManagementUseCase.UploadFile("empty.txt", "text/plain", new byte[0])))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> service.upload(ownerId,
                new com.ino.admin.file.api.FileManagementUseCase.UploadFile("image.txt", "image/png", new byte[] { 1 })))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> service.upload(ownerId,
                new com.ino.admin.file.api.FileManagementUseCase.UploadFile("fake.pdf", "application/pdf", "not-pdf".getBytes())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test void deletesObjectWhenMetadataPersistenceFails() {
        when(repository.save(any())).thenThrow(new IllegalStateException("db unavailable"));
        assertThatThrownBy(() -> service.upload(ownerId,
                new com.ino.admin.file.api.FileManagementUseCase.UploadFile("report.pdf", "application/pdf", "%PDF".getBytes())))
                .isInstanceOf(IllegalStateException.class);
        var key = ArgumentCaptor.forClass(String.class);
        verify(storage).delete(key.capture());
        assertThat(key.getValue()).isNotBlank();
    }

    @Test void hidesFilesOwnedByAnotherUser() {
        var file = StoredFile.create(UUID.randomUUID(), "report.pdf", "key", "application/pdf", 1,
                Instant.parse("2026-08-14T00:00:00Z"));
        when(repository.findById(file.id())).thenReturn(Optional.of(file));
        assertThatThrownBy(() -> service.download(ownerId, file.id())).isInstanceOf(FileNotFoundException.class);
    }
}
