package com.ino.admin.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/access-history")
class AccessHistoryController {
    private final AuditLogService service;
    AccessHistoryController(AuditLogService service) { this.service = service; }

    @GetMapping
    AccessHistoryPage find(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var resultPage = service.findAccessHistory(createdFrom, createdTo, page, size);
        return new AccessHistoryPage(resultPage.getContent().stream().map(AccessHistoryView::from).toList(), page, size,
                resultPage.getTotalElements(), resultPage.getTotalPages());
    }

    record AccessHistoryView(java.util.UUID id, String email, Instant createdAt) {
        static AccessHistoryView from(AuditLog log) {
            return new AccessHistoryView(log.id(), log.loginEmail(), log.createdAt());
        }
    }
    record AccessHistoryPage(List<AccessHistoryView> content, int page, int size, long totalElements, int totalPages) {}
}
