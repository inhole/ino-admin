package com.ino.admin.user;

import com.ino.admin.identity.api.PermissionCatalogUseCase;
import com.ino.admin.identity.api.RoleManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
class PermissionController {
    private final PermissionCatalogUseCase catalog;
    private final RoleManagementUseCase roleManagement;

    PermissionController(PermissionCatalogUseCase catalog, RoleManagementUseCase roleManagement) {
        this.catalog = catalog;
        this.roleManagement = roleManagement;
    }

    @GetMapping
    List<PermissionCatalogUseCase.RolePermissions> findAll() { return catalog.findAll(); }

    @GetMapping("/available")
    List<String> findAvailable() { return catalog.findAvailablePermissions(); }

    @PatchMapping("/roles/{role}")
    RoleManagementUseCase.UpdatedRole replacePermissions(@PathVariable String role,
            @Valid @RequestBody UpdatePermissionsRequest request) {
        return roleManagement.replacePermissions(role, request.permissions());
    }

    record UpdatePermissionsRequest(@NotNull List<String> permissions) {}
}
