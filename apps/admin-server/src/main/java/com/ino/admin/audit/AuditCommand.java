package com.ino.admin.audit;

import java.util.UUID;

public record AuditCommand(UUID actorId, String loginEmail, String loginDisplayName, String loginRole,
        String action, String resource, AuditResult result,
        int statusCode, String ipAddress, String userAgent, String traceId) {
    public static final String LOGIN_ACCOUNT_ATTRIBUTE = AuditCommand.class.getName() + ".loginAccount";
    public record LoginAccount(String email, String displayName, String role) {}
}
