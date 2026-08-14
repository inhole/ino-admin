package com.ino.admin.identity.api;

import java.util.List;

public interface PermissionCatalogUseCase {
    List<RolePermissions> findAll();
    List<String> findAvailablePermissions();

    record RolePermissions(String role, String displayName, boolean systemRole, boolean enabled, List<String> permissions) {}
}
