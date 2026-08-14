package com.ino.admin.identity.api;

import java.util.List;

public interface RoleManagementUseCase {
    UpdatedRole replacePermissions(String role, List<String> permissions);
    RoleView create(CreateRole command);
    RoleView rename(String role, String displayName);
    RoleView changeEnabled(String role, boolean enabled);
    record CreateRole(String role, String displayName, List<String> permissions) {}
    record RoleView(String role, String displayName, boolean systemRole, boolean enabled, List<String> permissions) {}
    record UpdatedRole(String role, List<String> permissions) {}
}
