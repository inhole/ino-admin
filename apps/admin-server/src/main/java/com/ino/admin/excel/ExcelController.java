package com.ino.admin.excel;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/excel")
class ExcelController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final UserExcelExporter exporter;
    ExcelController(UserExcelExporter exporter) { this.exporter = exporter; }

    @GetMapping("/users/export")
    ResponseEntity<byte[]> exportUsers() {
        var disposition = ContentDisposition.attachment()
                .filename("users.xlsx", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(exporter.export());
    }
}
