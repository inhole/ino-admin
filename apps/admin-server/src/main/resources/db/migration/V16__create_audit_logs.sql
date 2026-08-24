CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(16) NOT NULL,
    resource VARCHAR(500) NOT NULL,
    result VARCHAR(16) NOT NULL,
    status_code INTEGER NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    trace_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_actor_created_at ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_result_created_at ON audit_logs(action, result, created_at DESC);

INSERT INTO role_permissions (role_key, permission_key) VALUES
    ('SUPER_ADMIN', 'audit:read');

INSERT INTO menus (id, parent_id, label, route, icon, sort_order, required_permission, enabled, created_at, updated_at)
VALUES ('audit-logs', NULL, '감사 로그', '/audit-logs', 'history', 60, 'audit:read', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
