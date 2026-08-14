package com.ino.admin.menu.api;

import java.util.List;

public interface MenuManagementUseCase {
    List<MenuView> findAll();
    MenuView create(SaveMenu command);
    MenuView update(String id, SaveMenu command);

    record SaveMenu(String id, String parentId, String label, String route, String icon, int order,
                    String requiredPermission, boolean enabled) {}
    record MenuView(String id, String parentId, String label, String route, String icon, int order,
                    String requiredPermission, boolean enabled) {}
}
