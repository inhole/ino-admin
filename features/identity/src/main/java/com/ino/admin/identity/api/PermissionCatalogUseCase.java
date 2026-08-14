package com.ino.admin.identity.api;

import java.util.List;

public interface PermissionCatalogUseCase {
    List<RolePermissions> findAll();

    record RolePermissions(String role, List<String> permissions) {}
}
