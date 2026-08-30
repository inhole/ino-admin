package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionService {
    private final RoleRepository repository;

    public RolePermissionService(RoleRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<String> findPermissions(String role) {
        var found = repository.findById(role)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        return permissions(found);
    }

    @Transactional
    public TokenPermissions findTokenPermissionsForUpdate(String role) {
        var found = repository.findByIdForUpdate(role)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        return new TokenPermissions(found.enabled(), permissions(found));
    }

    private List<String> permissions(com.ino.admin.identity.domain.Role role) {
        if (!role.enabled()) return List.of();
        return role.permissions().stream().sorted().toList();
    }

    public record TokenPermissions(boolean enabled, List<String> permissions) {}
}
