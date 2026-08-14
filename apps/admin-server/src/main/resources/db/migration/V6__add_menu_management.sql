INSERT INTO menus (id, parent_id, label, route, icon, sort_order, required_permission, enabled, created_at, updated_at)
VALUES ('menu-management', NULL, '메뉴 관리', '/menu-management', 'menu', 40, 'menu:read', TRUE,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
