package com.ino.admin.user;

import com.ino.admin.identity.api.PermissionCatalogUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
class PermissionController {
    private final PermissionCatalogUseCase catalog;

    PermissionController(PermissionCatalogUseCase catalog) { this.catalog = catalog; }

    @GetMapping
    List<PermissionCatalogUseCase.RolePermissions> findAll() { return catalog.findAll(); }
}
