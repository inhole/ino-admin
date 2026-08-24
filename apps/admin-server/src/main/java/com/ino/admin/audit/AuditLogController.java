package com.ino.admin.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
class AuditLogController {
    private final AuditLogService service;
    AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    AuditLogPage find(@RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @Pattern(regexp = "[A-Z][A-Z0-9_]{1,99}") String action,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var resultPage = service.find(actorId, action, result, createdFrom, createdTo, page, size);
        return new AuditLogPage(resultPage.getContent().stream().map(AuditLogView::from).toList(), page, size,
                resultPage.getTotalElements(), resultPage.getTotalPages());
    }

    record AuditLogView(UUID id, UUID actorId, String action, String resource, AuditResult result,
            int statusCode, String ipAddress, String userAgent, String traceId, Instant createdAt) {
        static AuditLogView from(AuditLog log) {
            return new AuditLogView(log.id(), log.actorId(), log.action(), log.resource(), log.result(),
                    log.statusCode(), log.ipAddress(), log.userAgent(), log.traceId(), log.createdAt());
        }
    }
    record AuditLogPage(List<AuditLogView> content, int page, int size, long totalElements, int totalPages) {}
}
