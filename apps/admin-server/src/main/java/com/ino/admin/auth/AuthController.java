package com.ino.admin.auth;

import com.ino.admin.audit.AuditCommand;
import com.ino.admin.identity.api.LoginUseCase;
import com.ino.admin.identity.api.PasswordChangeUseCase;
import com.ino.admin.identity.api.RefreshTokenUseCase;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.Locale;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LoginUseCase loginService;
    private final RefreshTokenUseCase refreshTokenService;
    private final PasswordChangeUseCase passwordChangeService;

    public AuthController(LoginUseCase loginService, RefreshTokenUseCase refreshTokenService,
            PasswordChangeUseCase passwordChangeService) {
        this.loginService = loginService;
        this.refreshTokenService = refreshTokenService;
        this.passwordChangeService = passwordChangeService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var result = loginService.login(request.email(), request.password());
        servletRequest.setAttribute(AuditCommand.LOGIN_EMAIL_ATTRIBUTE,
                request.email().strip().toLowerCase(Locale.ROOT));
        return new LoginResponse(result.accessToken(), "Bearer", result.expiresInSeconds(), result.refreshToken());
    }

    @PostMapping("/refresh")
    LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var result = refreshTokenService.rotate(request.refreshToken());
        return new LoginResponse(result.accessToken(), "Bearer", result.expiresInSeconds(), result.refreshToken());
    }

    @PostMapping("/logout")
    void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    LoginUseCase.CurrentUser me(@AuthenticationPrincipal Jwt jwt) {
        return loginService.currentUser(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/password")
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordChangeRequest request) {
        passwordChangeService.change(UUID.fromString(jwt.getSubject()), request.currentPassword(), request.newPassword());
    }

    record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password
    ) {}

    record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {}
    record PasswordChangeRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 12, max = 128) String newPassword
    ) {}
    record LoginResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {}
}
