package com.ino.admin.audit;

import java.util.UUID;

public record AuditCommand(UUID actorId, String action, String resource, AuditResult result,
        int statusCode, String ipAddress, String userAgent, String traceId) {}
