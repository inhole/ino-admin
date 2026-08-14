package com.ino.admin.identity.application;

import com.ino.admin.identity.api.PermissionCatalogUseCase;
import com.ino.admin.identity.domain.UserRole;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PermissionCatalogService implements PermissionCatalogUseCase {
    @Override
    public List<PermissionCatalogUseCase.RolePermissions> findAll() {
        return Arrays.stream(UserRole.values())
                .map(role -> new PermissionCatalogUseCase.RolePermissions(role.name(),
                        com.ino.admin.identity.domain.RolePermissions.forRole(role)
                        .stream().map(permission -> permission.key()).sorted().toList()))
                .toList();
    }
}
