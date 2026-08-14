package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
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
        return repository.findById(role)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."))
                .permissions().stream().sorted().toList();
    }
}
