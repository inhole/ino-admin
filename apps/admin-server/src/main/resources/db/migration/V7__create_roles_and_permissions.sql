CREATE TABLE roles (
    role_key VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE role_permissions (
    role_key VARCHAR(50) NOT NULL REFERENCES roles(role_key) ON DELETE CASCADE,
    permission_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_key, permission_key)
);

INSERT INTO roles (role_key, display_name, system_role, created_at, updated_at) VALUES
    ('SUPER_ADMIN', '최고 관리자', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN', '관리자', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VIEWER', '조회자', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_key, permission_key) VALUES
    ('SUPER_ADMIN', 'user:read'), ('SUPER_ADMIN', 'user:create'), ('SUPER_ADMIN', 'user:update'),
    ('SUPER_ADMIN', 'permission:read'), ('SUPER_ADMIN', 'menu:read'), ('SUPER_ADMIN', 'menu:update'),
    ('ADMIN', 'user:read');
