package com.ino.admin.audit;

@FunctionalInterface
public interface AuditWriter {
    void write(AuditCommand command);
}
