package com.ino.admin.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
class AuditLog {
    @Id private UUID id;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "login_email", length = 320) private String loginEmail;
    @Column(name = "login_display_name", length = 100) private String loginDisplayName;
    @Column(name = "login_role", length = 100) private String loginRole;
    @Column(nullable = false, length = 100) private String action;
    @Column(nullable = false, length = 500) private String resource;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AuditResult result;
    @Column(name = "status_code", nullable = false) private int statusCode;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 512) private String userAgent;
    @Column(name = "trace_id", length = 100) private String traceId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AuditLog() {}

    static AuditLog create(AuditCommand command, Instant now) {
        var log = new AuditLog();
        log.id = UUID.randomUUID();
        log.actorId = command.actorId();
        log.loginEmail = command.loginEmail();
        log.loginDisplayName = command.loginDisplayName();
        log.loginRole = command.loginRole();
        log.action = command.action();
        log.resource = command.resource();
        log.result = command.result();
        log.statusCode = command.statusCode();
        log.ipAddress = command.ipAddress();
        log.userAgent = command.userAgent();
        log.traceId = command.traceId();
        log.createdAt = now;
        return log;
    }

    UUID id() { return id; }
    UUID actorId() { return actorId; }
    String loginEmail() { return loginEmail; }
    String loginDisplayName() { return loginDisplayName; }
    String loginRole() { return loginRole; }
    String action() { return action; }
    String resource() { return resource; }
    AuditResult result() { return result; }
    int statusCode() { return statusCode; }
    String ipAddress() { return ipAddress; }
    String userAgent() { return userAgent; }
    String traceId() { return traceId; }
    Instant createdAt() { return createdAt; }
}
