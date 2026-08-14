package com.ino.admin.identity.application;

import com.ino.admin.identity.api.PermissionCatalogUseCase;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import com.ino.admin.identity.domain.Permission;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PermissionCatalogService implements PermissionCatalogUseCase {
    private final RoleRepository repository;

    public PermissionCatalogService(RoleRepository repository) { this.repository = repository; }

    @Override
    public List<PermissionCatalogUseCase.RolePermissions> findAll() {
        return repository.findAllByOrderByKeyAsc().stream()
                .map(role -> new PermissionCatalogUseCase.RolePermissions(role.key(),
                        role.permissions().stream().sorted().toList()))
                .toList();
    }

    @Override
    public List<String> findAvailablePermissions() {
        return Arrays.stream(Permission.values()).map(Permission::key).sorted().toList();
    }
}
