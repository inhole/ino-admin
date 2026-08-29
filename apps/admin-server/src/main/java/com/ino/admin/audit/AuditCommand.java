package com.ino.admin.audit;

import java.util.UUID;

public record AuditCommand(UUID actorId, String loginEmail, String action, String resource, AuditResult result,
        int statusCode, String ipAddress, String userAgent, String traceId) {
    public static final String LOGIN_EMAIL_ATTRIBUTE = AuditCommand.class.getName() + ".loginEmail";
}
