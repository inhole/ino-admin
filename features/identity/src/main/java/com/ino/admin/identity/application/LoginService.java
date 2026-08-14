package com.ino.admin.identity.application;

import com.ino.admin.identity.api.AuthenticationFailedException;
import com.ino.admin.identity.api.LoginUseCase;
import com.ino.admin.identity.application.port.AccessTokenIssuer;
import com.ino.admin.identity.config.LoginSecurityProperties;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final LoginSecurityProperties securityProperties;
    private final Clock clock;
    private final RolePermissionService rolePermissionService;
    private final String dummyPasswordHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokenIssuer,
            RefreshTokenService refreshTokenService, LoginSecurityProperties securityProperties, Clock clock,
            RolePermissionService rolePermissionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.rolePermissionService = rolePermissionService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    @Override
    public LoginResult login(String email, String password) {
        var normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailForUpdate(normalizedEmail).orElse(null);
        var passwordHash = user == null ? dummyPasswordHash : user.passwordHash();
        var passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (user == null || !passwordMatches || user.status() != UserStatus.ACTIVE) {
            if (user != null && !passwordMatches && user.status() == UserStatus.ACTIVE) {
                user.recordFailedLogin(securityProperties.getMaxFailedAttempts(), Instant.now(clock));
            }
            throw new AuthenticationFailedException();
        }
        user.recordSuccessfulLogin(Instant.now(clock));
        var token = accessTokenIssuer.issue(user.id(), user.role().name(),
                rolePermissionService.findPermissions(user.role().name()));
        var refreshToken = refreshTokenService.issue(user);
        return new LoginResult(token.value(), token.expiresInSeconds(), refreshToken.rawToken());
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentUser currentUser(UUID userId) {
        var user = userRepository.findById(userId)
                .filter(found -> found.status() == UserStatus.ACTIVE)
                .orElseThrow(AuthenticationFailedException::new);
        return new CurrentUser(user.id(), user.email(), user.displayName(), user.status().name(), user.role().name(),
                rolePermissionService.findPermissions(user.role().name()));
    }

}
