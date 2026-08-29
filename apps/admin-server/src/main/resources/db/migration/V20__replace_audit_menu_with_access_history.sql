INSERT INTO role_permissions (role_key, permission_key)
VALUES ('SUPER_ADMIN', 'access-history:read')
ON CONFLICT DO NOTHING;

UPDATE menus
SET id = 'access-history',
    parent_id = 'users',
    label = '접속 이력',
    route = '/access-history',
    required_permission = 'access-history:read',
    sort_order = 20,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'audit-logs';

UPDATE menus
SET parent_id = 'users',
    sort_order = 10,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'permissions';

DELETE FROM role_permissions WHERE permission_key = 'audit:read';
