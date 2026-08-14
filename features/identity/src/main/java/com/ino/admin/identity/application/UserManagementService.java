package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
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
            throw new BusinessException("SELF_DISABLE_NOT_ALLOWED", "자기 계정은 비활성화할 수 없습니다.");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        user.changeStatus(requested, Instant.now(clock));
        if (requested == UserStatus.DISABLED) refreshTokenService.revokeAllForUser(user.id());
        return new UpdatedUser(user.id(), user.status().name());
    }

    @Override
    @Transactional
    public UpdatedProfile updateProfile(java.util.UUID actorId, java.util.UUID userId, UpdateProfile command) {
        if (actorId.equals(userId)) {
            throw new BusinessException("SELF_ROLE_CHANGE_NOT_ALLOWED", "자기 계정의 역할은 변경할 수 없습니다.");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        var role = parseAssignableRole(command.role());
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
            throw new BusinessException("INVALID_USER_STATUS", "변경 가능한 상태는 ACTIVE 또는 DISABLED입니다.");
        }
    }

    @Override
    @Transactional
    public CreatedUser create(CreateUser command) {
        var email = command.email().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }
        var violations = PasswordPolicy.violations(command.password());
        if (!violations.isEmpty()) {
            throw new BusinessException("PASSWORD_POLICY_VIOLATION", String.join(" ", violations));
        }
        var role = parseAssignableRole(command.role());
        var user = User.create(email, passwordEncoder.encode(command.password()), command.displayName(), role,
                Instant.now(clock));
        userRepository.save(user);
        return new CreatedUser(user.id(), user.email(), user.displayName(), user.status().name(), user.role());
    }

    private String parseAssignableRole(String role) {
        var normalized = role == null ? "" : role.strip();
        if (normalized.equals(UserRole.SUPER_ADMIN.name()) || !roleRepository.existsById(normalized))
            throw new BusinessException("INVALID_USER_ROLE", "할당 가능한 역할을 선택해야 합니다.");
        return normalized;
    }
}
