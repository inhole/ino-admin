package com.ino.admin.file.application;

import com.ino.admin.file.api.FileManagementUseCase;
import com.ino.admin.file.api.FileNotFoundException;
import com.ino.admin.file.api.InvalidFileException;
import com.ino.admin.file.application.port.FileStorage;
import com.ino.admin.file.config.FileStorageProperties;
import com.ino.admin.file.domain.FileStatus;
import com.ino.admin.file.domain.StoredFile;
import com.ino.admin.file.infrastructure.persistence.StoredFileRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class FileManagementService implements FileManagementUseCase {
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf", "image/png", ".png", "image/jpeg", ".jpg", "text/plain", ".txt");
    private final StoredFileRepository repository;
    private final FileStorage storage;
    private final FileStorageProperties properties;
    private final Clock clock;
    private final FileDeletionCoordinator deletionCoordinator;

    public FileManagementService(StoredFileRepository repository, FileStorage storage, FileStorageProperties properties, Clock clock,
            FileDeletionCoordinator deletionCoordinator) {
        this.repository = repository; this.storage = storage; this.properties = properties; this.clock = clock;
        this.deletionCoordinator = deletionCoordinator;
    }

    @Override @Transactional
    public StoredFile upload(UUID ownerId, UploadFile command) {
        validate(command);
        var name = sanitizeName(command.originalName());
        var key = UUID.randomUUID().toString();
        storage.save(key, command.content());
        try {
            var saved = repository.saveAndFlush(com.ino.admin.file.domain.StoredFile.create(ownerId, name, key,
                    command.contentType(), command.content().length, Instant.now(clock)));
            return new StoredFile(saved.id(), saved.originalName(), saved.contentType(), saved.size());
        } catch (RuntimeException exception) {
            storage.delete(key);
            throw exception;
        }
    }

    @Override @Transactional(readOnly = true)
    public FileDownload download(UUID requesterId, UUID fileId) {
        var file = repository.findById(fileId).filter(found -> found.ownerId().equals(requesterId) && found.isReady())
                .orElseThrow(FileNotFoundException::new);
        return new FileDownload(file.originalName(), file.contentType(), storage.load(file.storageKey()));
    }

    @Override @Transactional(readOnly = true)
    public FilePage list(UUID ownerId, FileListQuery query, int page, int size) {
        var direction = query.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        var property = switch (query.sort()) {
            case CREATED_AT -> "createdAt";
            case ORIGINAL_NAME -> "originalName";
            case SIZE -> "size";
        };
        var pageable = PageRequest.of(page, size, Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));
        var name = query.name() == null || query.name().isBlank() ? null : query.name().strip();
        var result = repository.findAll(fileListSpecification(ownerId, name, query), pageable);
        return new FilePage(result.getContent().stream().map(file -> new FileSummary(file.id(), file.originalName(),
                file.contentType(), file.size(), file.createdAt())).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public void delete(UUID requesterId, UUID fileId) {
        deletionCoordinator.delete(requesterId, fileId);
    }

    private Specification<com.ino.admin.file.domain.StoredFile> fileListSpecification(
            UUID ownerId, String name, FileListQuery query) {
        return (root, ignored, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("ownerId"), ownerId));
            predicates.add(criteriaBuilder.equal(root.get("status"), FileStatus.READY));
            if (name != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("originalName")),
                        "%" + name.toLowerCase(Locale.ROOT) + "%"));
            }
            if (query.contentType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("contentType"), query.contentType()));
            }
            if (query.createdFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), query.createdTo()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validate(UploadFile command) {
        if (command.content() == null || command.content().length == 0) throw new InvalidFileException("빈 파일은 업로드할 수 없습니다.");
        if (command.content().length > properties.getMaxSize().toBytes()) throw new InvalidFileException("파일 크기 제한을 초과했습니다.");
        var extension = ALLOWED_TYPES.get(command.contentType());
        if (extension == null) throw new InvalidFileException("허용되지 않은 파일 형식입니다.");
        var lowerName = command.originalName() == null ? "" : command.originalName().toLowerCase(java.util.Locale.ROOT);
        if (!(lowerName.endsWith(extension) || (extension.equals(".jpg") && lowerName.endsWith(".jpeg"))))
            throw new InvalidFileException("파일 확장자와 형식이 일치하지 않습니다.");
        if (!hasValidSignature(command.contentType(), command.content()))
            throw new InvalidFileException("파일 내용과 형식이 일치하지 않습니다.");
    }

    private boolean hasValidSignature(String contentType, byte[] content) {
        return switch (contentType) {
            case "application/pdf" -> startsWith(content, new int[] { 0x25, 0x50, 0x44, 0x46 });
            case "image/png" -> startsWith(content, new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
            case "image/jpeg" -> startsWith(content, new int[] { 0xFF, 0xD8, 0xFF });
            case "text/plain" -> java.util.stream.IntStream.range(0, content.length).noneMatch(index -> content[index] == 0);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, int[] signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++)
            if (Byte.toUnsignedInt(content[index]) != signature[index]) return false;
        return true;
    }

    private String sanitizeName(String originalName) {
        var name = PathSupport.fileName(originalName);
        if (name.isBlank() || name.length() > 255) throw new InvalidFileException("파일 이름이 올바르지 않습니다.");
        return name;
    }

    private static final class PathSupport {
        static String fileName(String value) { return value.replace('\\', '/').substring(value.replace('\\', '/').lastIndexOf('/') + 1).strip(); }
    }
}
