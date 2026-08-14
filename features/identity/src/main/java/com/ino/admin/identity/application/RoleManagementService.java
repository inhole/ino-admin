package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.RoleManagementUseCase;
import com.ino.admin.identity.domain.Permission;
import com.ino.admin.identity.domain.UserRole;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleManagementService implements RoleManagementUseCase {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    public RoleManagementService(RoleRepository roleRepository, UserRepository userRepository,
            RefreshTokenService refreshTokenService, Clock clock) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UpdatedRole replacePermissions(String roleKey, List<String> permissions) {
        if ("SUPER_ADMIN".equals(roleKey))
            throw new BusinessException("SYSTEM_ROLE_PROTECTED", "최고 관리자 권한은 변경할 수 없습니다.");
        var role = roleRepository.findById(roleKey)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."));
        var allowed = Arrays.stream(Permission.values()).map(Permission::key).collect(Collectors.toSet());
        var requested = Set.copyOf(permissions);
        if (!allowed.containsAll(requested))
            throw new BusinessException("INVALID_PERMISSION", "등록되지 않은 권한이 포함되어 있습니다.");
        role.replacePermissions(requested, Instant.now(clock));
        userRepository.findAllByRole(UserRole.valueOf(roleKey))
                .forEach(user -> refreshTokenService.revokeAllForUser(user.id()));
        return new UpdatedRole(role.key(), role.permissions().stream().sorted().toList());
    }
}
