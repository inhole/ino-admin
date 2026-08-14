package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.UserManagementUseCase;
import com.ino.admin.identity.domain.PasswordPolicy;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserRole;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
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

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
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
        return new CreatedUser(user.id(), user.email(), user.displayName(), user.status().name(), user.role().name());
    }

    private UserRole parseAssignableRole(String role) {
        try {
            var parsed = UserRole.valueOf(role);
            if (parsed == UserRole.SUPER_ADMIN) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("INVALID_USER_ROLE", "생성 가능한 역할은 ADMIN 또는 VIEWER입니다.");
        }
    }
}
