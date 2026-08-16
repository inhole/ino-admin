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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FileManagementServiceTest {
    @Mock StoredFileRepository repository;
    @Mock FileStorage storage;
    @Mock FileDeletionCoordinator deletionCoordinator;
    private FileManagementService service;
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        var properties = new FileStorageProperties();
        service = new FileManagementService(repository, storage, properties,
                Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC), deletionCoordinator);
    }

    @Test void storesSafeMetadataAndContent() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("db unavailable"));
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

    @Test void listsOnlyOwnersFiles() {
        var file = StoredFile.create(ownerId, "report.pdf", "key", "application/pdf", 4,
                Instant.parse("2026-08-14T00:00:00Z"));
        when(repository.search(org.mockito.ArgumentMatchers.eq(ownerId),
                org.mockito.ArgumentMatchers.eq(com.ino.admin.file.domain.FileStatus.READY),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(file)));
        var query = new com.ino.admin.file.api.FileManagementUseCase.FileListQuery(null, null, null, null,
                com.ino.admin.file.api.FileManagementUseCase.FileSort.CREATED_AT,
                com.ino.admin.file.api.FileManagementUseCase.SortDirection.DESC);
        assertThat(service.list(ownerId, query, 0, 20).content()).extracting("originalName").containsExactly("report.pdf");
    }

    @Test void appliesFileFiltersAndWhitelistedSort() {
        var from = Instant.parse("2026-08-01T00:00:00Z");
        var to = Instant.parse("2026-09-01T00:00:00Z");
        when(repository.search(org.mockito.ArgumentMatchers.eq(ownerId),
                org.mockito.ArgumentMatchers.eq(com.ino.admin.file.domain.FileStatus.READY),
                org.mockito.ArgumentMatchers.eq("report"), org.mockito.ArgumentMatchers.eq("application/pdf"),
                org.mockito.ArgumentMatchers.eq(from), org.mockito.ArgumentMatchers.eq(to), any()))
                .thenReturn(Page.empty());
        var query = new com.ino.admin.file.api.FileManagementUseCase.FileListQuery("  report  ", "application/pdf", from, to,
                com.ino.admin.file.api.FileManagementUseCase.FileSort.ORIGINAL_NAME,
                com.ino.admin.file.api.FileManagementUseCase.SortDirection.ASC);

        service.list(ownerId, query, 0, 20);

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(org.mockito.ArgumentMatchers.eq(ownerId),
                org.mockito.ArgumentMatchers.eq(com.ino.admin.file.domain.FileStatus.READY),
                org.mockito.ArgumentMatchers.eq("report"), org.mockito.ArgumentMatchers.eq("application/pdf"),
                org.mockito.ArgumentMatchers.eq(from), org.mockito.ArgumentMatchers.eq(to), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("originalName").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test void deletesOwnedObjectAndMetadata() {
        var fileId = UUID.randomUUID();
        service.delete(ownerId, fileId);
        verify(deletionCoordinator).delete(ownerId, fileId);
    }
}
