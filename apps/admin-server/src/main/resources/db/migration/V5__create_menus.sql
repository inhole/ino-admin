CREATE TABLE menus (
    id VARCHAR(50) PRIMARY KEY,
    parent_id VARCHAR(50) REFERENCES menus(id),
    label VARCHAR(100) NOT NULL,
    route VARCHAR(255) NOT NULL,
    icon VARCHAR(50) NOT NULL,
    sort_order INTEGER NOT NULL,
    required_permission VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_menus_parent_order UNIQUE NULLS NOT DISTINCT (parent_id, sort_order)
);

INSERT INTO menus (id, parent_id, label, route, icon, sort_order, required_permission, enabled, created_at, updated_at)
VALUES
    ('dashboard', NULL, '대시보드', '/', 'layout-dashboard', 10, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('users', NULL, '사용자', '/users', 'users', 20, 'user:read', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('permissions', NULL, '권한', '/permissions', 'key-round', 30, 'permission:read', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
