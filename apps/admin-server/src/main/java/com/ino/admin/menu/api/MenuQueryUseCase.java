package com.ino.admin.menu.api;

import java.util.List;
import java.util.Set;

public interface MenuQueryUseCase {
    List<MenuItem> findAccessibleMenus(Set<String> permissions);

    record MenuItem(String id, String label, String route, String icon, int order, List<MenuItem> children) {}
}
