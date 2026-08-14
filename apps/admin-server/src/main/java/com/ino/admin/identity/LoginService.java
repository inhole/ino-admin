package com.ino.admin.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final String dummyPasswordHash;

    LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokenIssuer,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResult login(String email, String password) {
        var normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmail(normalizedEmail).orElse(null);
        var passwordHash = user == null ? dummyPasswordHash : user.passwordHash();
        var passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (user == null || !passwordMatches || user.status() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException();
        }
        var token = accessTokenIssuer.issue(user.id());
        var refreshToken = refreshTokenService.issue(user);
        return new LoginResult(token.value(), token.expiresInSeconds(), refreshToken.rawToken());
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser(UUID userId) {
        var user = userRepository.findById(userId)
                .filter(found -> found.status() == UserStatus.ACTIVE)
                .orElseThrow(AuthenticationFailedException::new);
        return new CurrentUser(user.id(), user.email(), user.displayName(), user.status().name());
    }

    public record LoginResult(String accessToken, long expiresInSeconds, String refreshToken) {}
    public record CurrentUser(UUID id, String email, String displayName, String status) {}
}
