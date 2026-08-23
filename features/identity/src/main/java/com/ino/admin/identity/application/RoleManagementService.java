package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.RoleManagementUseCase;
import com.ino.admin.identity.domain.Permission;
import com.ino.admin.identity.domain.Role;
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
        userRepository.findAllByRole(roleKey)
                .forEach(user -> refreshTokenService.revokeAllForUser(user.id()));
        return new UpdatedRole(role.key(), role.permissions().stream().sorted().toList());
    }

    @Override @Transactional
    public RoleView create(CreateRole command) {
        var key = command.role().strip().toUpperCase(java.util.Locale.ROOT);
        if (!key.matches("[A-Z][A-Z0-9_]{2,49}")) throw new BusinessException("INVALID_ROLE_KEY", "역할 키 형식이 올바르지 않습니다.");
        if (roleRepository.existsById(key)) throw new BusinessException("ROLE_ALREADY_EXISTS", "이미 존재하는 역할입니다.");
        var permissions = validatedPermissions(command.permissions());
        return view(roleRepository.save(Role.create(key, command.displayName(), permissions, Instant.now(clock))));
    }

    @Override @Transactional
    public RoleView rename(String roleKey, String displayName) {
        var role = editableCustomRole(roleKey); role.rename(displayName, Instant.now(clock)); return view(role);
    }

    @Override @Transactional
    public RoleView changeEnabled(String roleKey, boolean enabled) {
        var users = enabled ? List.<com.ino.admin.identity.domain.User>of()
                : userRepository.findAllByRoleForUpdate(roleKey);
        var role = editableCustomRoleForUpdate(roleKey); role.changeEnabled(enabled, Instant.now(clock));
        if (!enabled) {
            var currentUserIds = userRepository.findIdsByRoleOrderById(roleKey);
            java.util.stream.Stream.concat(users.stream().map(com.ino.admin.identity.domain.User::id),
                            currentUserIds.stream())
                    .distinct()
                    .sorted()
                    .forEach(refreshTokenService::revokeAllForUser);
        }
        return view(role);
    }

    private Role editableCustomRoleForUpdate(String key) {
        var role = roleRepository.findByIdForUpdate(key)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."));
        if (role.systemRole()) throw new BusinessException("SYSTEM_ROLE_PROTECTED", "시스템 역할은 변경할 수 없습니다.");
        return role;
    }

    private Role editableCustomRole(String key) {
        var role = roleRepository.findById(key).orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다."));
        if (role.systemRole()) throw new BusinessException("SYSTEM_ROLE_PROTECTED", "시스템 역할은 변경할 수 없습니다.");
        return role;
    }

    private Set<String> validatedPermissions(List<String> permissions) {
        var allowed = Arrays.stream(Permission.values()).map(Permission::key).collect(Collectors.toSet());
        var requested = Set.copyOf(permissions);
        if (!allowed.containsAll(requested)) throw new BusinessException("INVALID_PERMISSION", "등록되지 않은 권한이 포함되어 있습니다.");
        return requested;
    }

    private RoleView view(Role role) { return new RoleView(role.key(), role.displayName(), role.systemRole(), role.enabled(), role.permissions().stream().sorted().toList()); }
}
