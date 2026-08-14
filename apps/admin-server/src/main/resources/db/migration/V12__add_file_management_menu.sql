INSERT INTO menus (id, parent_id, label, route, icon, sort_order, required_permission, enabled, created_at, updated_at)
VALUES ('files', NULL, '파일 관리', '/files', 'file', 50, 'file:read', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
