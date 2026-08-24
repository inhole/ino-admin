package com.ino.admin.excel;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/excel")
class ExcelController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final UserExcelExporter exporter;
    private final UserExcelImporter importer;
    private final UserExcelTemplate template;
    ExcelController(UserExcelExporter exporter, UserExcelImporter importer, UserExcelTemplate template) {
        this.exporter = exporter; this.importer = importer; this.template = template;
    }

    @GetMapping("/users/export")
    ResponseEntity<byte[]> exportUsers() {
        var disposition = ContentDisposition.attachment()
                .filename("users.xlsx", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(exporter.export());
    }

    @GetMapping("/users/import-template")
    ResponseEntity<byte[]> importTemplate() {
        var disposition = ContentDisposition.attachment().filename("users-import-template.xlsx", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(XLSX).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff").body(template.create());
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UserExcelImporter.ImportResult importUsers(@RequestPart("file") MultipartFile file) {
        return importer.importUsers(file);
    }
}
