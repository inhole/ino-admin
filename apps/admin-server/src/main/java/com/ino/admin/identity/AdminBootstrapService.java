package com.ino.admin.identity;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminBootstrapService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    enum Result { CREATED, ALREADY_EXISTS }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    Result bootstrap(String email, String password, String displayName) {
        var normalizedEmail = requireEmail(email);
        requireDisplayName(displayName);
        var violations = PasswordPolicy.violations(password);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("초기 관리자 비밀번호 정책 위반: " + String.join(" ", violations));
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            return Result.ALREADY_EXISTS;
        }
        var user = User.createInitialAdmin(
                normalizedEmail,
                passwordEncoder.encode(password),
                displayName,
                Instant.now(clock)
        );
        userRepository.save(user);
        return Result.CREATED;
    }

    private static String requireEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 320 || !EMAIL_PATTERN.matcher(email.strip()).matches()) {
            throw new IllegalStateException("초기 관리자 이메일 설정이 올바르지 않습니다.");
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static void requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > 100) {
            throw new IllegalStateException("초기 관리자 표시 이름 설정이 올바르지 않습니다.");
        }
    }
}
