CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL CHECK (size > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_stored_files_owner_created_at ON stored_files(owner_id, created_at DESC);

INSERT INTO role_permissions (role_key, permission_key) VALUES
    ('SUPER_ADMIN', 'file:read'), ('SUPER_ADMIN', 'file:write'),
    ('ADMIN', 'file:read'), ('ADMIN', 'file:write');
