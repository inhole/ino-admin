package com.ino.admin.identity.application;

import com.ino.spring.modules.core.BusinessException;
import com.ino.admin.error.ErrorCode;
import com.ino.admin.identity.api.UserManagementUseCase;
import com.ino.admin.identity.domain.PasswordPolicy;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserRole;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService implements UserManagementUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final RefreshTokenService refreshTokenService;
    private final RoleRepository roleRepository;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock,
            RefreshTokenService refreshTokenService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.refreshTokenService = refreshTokenService;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UpdatedUser changeStatus(java.util.UUID actorId, java.util.UUID userId, String status) {
        var requested = parseStatus(status);
        if (actorId.equals(userId) && requested == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.SELF_DISABLE_NOT_ALLOWED);
        }
        var activeSuperAdmins = requested == UserStatus.DISABLED
                ? userRepository.findAllActiveSuperAdminsForUpdate()
                : java.util.List.<User>of();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (requested == UserStatus.DISABLED) protectLastActiveSuperAdmin(user, activeSuperAdmins);
        user.changeStatus(requested, Instant.now(clock));
        if (requested == UserStatus.DISABLED) refreshTokenService.revokeAllForUser(user.id());
        return new UpdatedUser(user.id(), user.status().name());
    }

    @Override
    @Transactional
    public UpdatedProfile updateProfile(java.util.UUID actorId, java.util.UUID userId, UpdateProfile command) {
        if (actorId.equals(userId)) {
            throw new BusinessException(ErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED);
        }
        var activeSuperAdmins = userRepository.findAllActiveSuperAdminsForUpdate();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var role = parseAssignableRoleForUpdate(command.role());
        if (!role.equals(user.role())) protectLastActiveSuperAdmin(user, activeSuperAdmins);
        user.updateProfile(command.displayName(), role, Instant.now(clock));
        refreshTokenService.revokeAllForUser(user.id());
        return new UpdatedProfile(user.id(), user.displayName(), user.role());
    }

    private UserStatus parseStatus(String status) {
        try {
            var parsed = UserStatus.valueOf(status);
            if (parsed == UserStatus.LOCKED) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS, "변경 가능한 상태는 ACTIVE 또는 DISABLED입니다.");
        }
    }

    @Override
    @Transactional
    public CreatedUser create(CreateUser command) {
        var email = command.email().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        var violations = PasswordPolicy.violations(command.password());
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, String.join(" ", violations));
        }
        var role = parseAssignableRoleForUpdate(command.role());
        var user = User.create(email, passwordEncoder.encode(command.password()), command.displayName(), role,
                Instant.now(clock));
        userRepository.save(user);
        return new CreatedUser(user.id(), user.email(), user.displayName(), user.status().name(), user.role());
    }

    private String parseAssignableRole(String role) {
        var normalized = role == null ? "" : role.strip();
        if (normalized.equals(UserRole.SUPER_ADMIN.name()) || roleRepository.findById(normalized).filter(found -> found.enabled()).isEmpty())
            throw new BusinessException(ErrorCode.INVALID_USER_ROLE);
        return normalized;
    }

    private String parseAssignableRoleForUpdate(String role) {
        var normalized = role == null ? "" : role.strip();
        if (normalized.equals(UserRole.SUPER_ADMIN.name())
                || roleRepository.findByIdForUpdate(normalized).filter(found -> found.enabled()).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_USER_ROLE);
        }
        return normalized;
    }

    private void protectLastActiveSuperAdmin(User user, java.util.List<User> activeSuperAdmins) {
        if (!UserRole.SUPER_ADMIN.name().equals(user.role()) || user.status() != UserStatus.ACTIVE) return;
        if (activeSuperAdmins.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_SUPER_ADMIN_PROTECTED);
        }
    }
}
