package com.ino.admin.audit;

import java.util.Map;
import java.util.UUID;

public record AuditActor(UUID id, Map<String, String> attributes) {
    public AuditActor {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
