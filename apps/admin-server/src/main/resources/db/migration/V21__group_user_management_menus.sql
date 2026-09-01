INSERT INTO menus (id, parent_id, label, route, icon, sort_order, required_permission, enabled, created_at, updated_at)
VALUES ('user-management', NULL, '사용자 관리', '/users', 'users', 200, 'user:read', TRUE,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE menus
SET parent_id = 'user-management',
    label = '사용자',
    sort_order = 10,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'users';

UPDATE menus
SET parent_id = 'user-management',
    sort_order = CASE id
        WHEN 'permissions' THEN 20
        WHEN 'access-history' THEN 30
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN ('permissions', 'access-history');

UPDATE menus
SET sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'user-management';
