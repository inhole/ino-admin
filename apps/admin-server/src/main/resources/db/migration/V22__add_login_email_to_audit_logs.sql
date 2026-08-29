ALTER TABLE audit_logs
    ADD COLUMN login_email VARCHAR(320);

CREATE INDEX idx_audit_logs_login_history
    ON audit_logs(created_at DESC)
    WHERE action = 'AUTH_LOGIN' AND result = 'SUCCESS' AND login_email IS NOT NULL;
