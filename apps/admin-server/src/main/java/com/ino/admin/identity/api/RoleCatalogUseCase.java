package com.ino.admin.identity.api;

import java.util.List;

public interface RoleCatalogUseCase {
    List<RoleOption> findActiveRoles();

    record RoleOption(String role, String displayName) {}
}
