ALTER TABLE audit_logs
    ADD COLUMN login_display_name VARCHAR(100),
    ADD COLUMN login_role VARCHAR(100);
