package com.ino.admin.menu.application;

import com.ino.admin.menu.api.MenuQueryUseCase;
import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MenuQueryService implements MenuQueryUseCase {
    private final MenuRepository repository;

    public MenuQueryService(MenuRepository repository) { this.repository = repository; }

    @Override
    public List<MenuItem> findAccessibleMenus(Set<String> permissions) {
        var granted = permissions == null ? Set.<String>of() : Set.copyOf(permissions);
        var menus = repository.findAllByEnabledTrueOrderBySortOrderAsc();
        var ids = menus.stream().map(Menu::id).collect(java.util.stream.Collectors.toSet());
        if (menus.stream().anyMatch(menu -> menu.parentId() != null && !ids.contains(menu.parentId()))) {
            throw new IllegalStateException("활성 메뉴의 부모가 존재하지 않습니다.");
        }
        menus.forEach(menu -> validateAncestors(menu, menus, new HashSet<>()));
        return menus.stream()
                .filter(menu -> menu.parentId() == null)
                .filter(menu -> menu.requiredPermission() == null || granted.contains(menu.requiredPermission()))
                .map(menu -> toItem(menu, menus, granted, new HashSet<>()))
                .toList();
    }

    private MenuItem toItem(Menu menu, List<Menu> menus, Set<String> granted, Set<String> ancestors) {
        if (!ancestors.add(menu.id())) throw new IllegalStateException("메뉴 tree에 순환이 존재합니다.");
        var children = menus.stream().filter(candidate -> menu.id().equals(candidate.parentId()))
                .filter(candidate -> candidate.requiredPermission() == null || granted.contains(candidate.requiredPermission()))
                .map(candidate -> toItem(candidate, menus, granted, new HashSet<>(ancestors))).toList();
        return new MenuItem(menu.id(), menu.label(), menu.route(), menu.icon(), menu.sortOrder(), children);
    }

    private void validateAncestors(Menu menu, List<Menu> menus, Set<String> visited) {
        if (!visited.add(menu.id())) throw new IllegalStateException("메뉴 tree에 순환이 존재합니다.");
        if (menu.parentId() == null) return;
        menus.stream().filter(candidate -> candidate.id().equals(menu.parentId())).findFirst()
                .ifPresent(parent -> validateAncestors(parent, menus, visited));
    }
}
