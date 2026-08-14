package com.ino.admin.auth;

import com.ino.admin.identity.LoginService;
import com.ino.admin.identity.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(LoginService loginService, RefreshTokenService refreshTokenService) {
        this.loginService = loginService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = loginService.login(request.email(), request.password());
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
    LoginService.CurrentUser me(@AuthenticationPrincipal Jwt jwt) {
        return loginService.currentUser(UUID.fromString(jwt.getSubject()));
    }

    record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password
    ) {}

    record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {}
    record LoginResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {}
}
