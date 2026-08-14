package com.ino.admin.menu.application;

import com.ino.admin.menu.api.MenuQueryUseCase;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MenuQueryService implements MenuQueryUseCase {
    private static final List<MenuDefinition> CATALOG = List.of(
            new MenuDefinition("dashboard", "대시보드", "/", "layout-dashboard", 10, null),
            new MenuDefinition("users", "사용자", "/users", "users", 20, "user:read"),
            new MenuDefinition("permissions", "권한", "/permissions", "key-round", 30, "permission:read")
    );

    @Override
    public List<MenuItem> findAccessibleMenus(Set<String> permissions) {
        var granted = permissions == null ? Set.<String>of() : Set.copyOf(permissions);
        return CATALOG.stream()
                .filter(menu -> menu.requiredPermission() == null || granted.contains(menu.requiredPermission()))
                .map(menu -> new MenuItem(menu.id(), menu.label(), menu.route(), menu.icon(), menu.order(), List.of()))
                .toList();
    }

    private record MenuDefinition(String id, String label, String route, String icon, int order,
                                  String requiredPermission) {}
}
