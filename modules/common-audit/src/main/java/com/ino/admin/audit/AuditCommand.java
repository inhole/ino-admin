package com.ino.admin.audit;

import java.util.Map;

public record AuditCommand(AuditActor actor, String action, String resource, AuditResult result,
        int statusCode, String traceId, Map<String, String> contextAttributes) {
    public AuditCommand {
        actor = actor == null ? new AuditActor(null, Map.of()) : actor;
        contextAttributes = contextAttributes == null ? Map.of() : Map.copyOf(contextAttributes);
    }
}
