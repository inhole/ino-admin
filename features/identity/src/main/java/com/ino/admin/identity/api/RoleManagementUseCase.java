package com.ino.admin.identity.api;

import java.util.List;

public interface RoleManagementUseCase {
    UpdatedRole replacePermissions(String role, List<String> permissions);
    record UpdatedRole(String role, List<String> permissions) {}
}
