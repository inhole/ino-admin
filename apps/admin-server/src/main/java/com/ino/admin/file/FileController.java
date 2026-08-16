package com.ino.admin.file;

import com.ino.admin.file.api.FileManagementUseCase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileManagementUseCase files;
    public FileController(FileManagementUseCase files) { this.files = files; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FileManagementUseCase.StoredFile> upload(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) throws IOException {
        var stored = files.upload(UUID.fromString(jwt.getSubject()),
                new FileManagementUseCase.UploadFile(file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        return ResponseEntity.status(201).body(stored);
    }

    @GetMapping("/{fileId}/content")
    ResponseEntity<ByteArrayResource> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fileId) {
        var file = files.download(UUID.fromString(jwt.getSubject()), fileId);
        var disposition = ContentDisposition.attachment().filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.content().length).body(new ByteArrayResource(file.content()));
    }

    @GetMapping
    FileManagementUseCase.FilePage list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 255) String name,
            @RequestParam(required = false)
            @Pattern(regexp = "application/pdf|image/png|image/jpeg|text/plain") String contentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "createdAt|originalName|size") String sort,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String direction) {
        var query = new FileManagementUseCase.FileListQuery(name, contentType, createdFrom, createdTo,
                toFileSort(sort), direction.equals("asc")
                        ? FileManagementUseCase.SortDirection.ASC
                        : FileManagementUseCase.SortDirection.DESC);
        return files.list(UUID.fromString(jwt.getSubject()), query, page, size);
    }

    @DeleteMapping("/{fileId}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID fileId) {
        files.delete(UUID.fromString(jwt.getSubject()), fileId);
        return ResponseEntity.noContent().build();
    }

    private FileManagementUseCase.FileSort toFileSort(String sort) {
        return switch (sort) {
            case "originalName" -> FileManagementUseCase.FileSort.ORIGINAL_NAME;
            case "size" -> FileManagementUseCase.FileSort.SIZE;
            default -> FileManagementUseCase.FileSort.CREATED_AT;
        };
    }
}
