package com.ino.admin.identity.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.AuthenticationFailedException;
import com.ino.admin.identity.api.PasswordChangeUseCase;
import com.ino.admin.identity.domain.PasswordPolicy;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService implements PasswordChangeUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    public PasswordChangeService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
    }

    @Transactional
    @Override
    public void change(UUID userId, String currentPassword, String newPassword) {
        var user = userRepository.findById(userId)
                .filter(found -> found.status() == UserStatus.ACTIVE)
                .orElseThrow(AuthenticationFailedException::new);
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, user.passwordHash())) {
            throw new BusinessException("PASSWORD_REUSE_NOT_ALLOWED", "현재 비밀번호와 다른 비밀번호를 사용해야 합니다.");
        }
        var violations = PasswordPolicy.violations(newPassword);
        if (!violations.isEmpty()) {
            throw new BusinessException("PASSWORD_POLICY_VIOLATION", String.join(" ", violations));
        }

        user.changePassword(passwordEncoder.encode(newPassword), Instant.now(clock));
        refreshTokenService.revokeAllForUser(user.id());
    }
}
