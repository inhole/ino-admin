package com.ino.admin.file;

import com.ino.admin.file.api.FileManagementUseCase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
