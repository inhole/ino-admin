package com.ino.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@Transactional
@AutoConfigureMockMvc
@SpringBootTest
class AuthFlowIntegrationTest {
    private static final String EMAIL = "login@example.com";
    private static final String PASSWORD = "Login-Password-2026!";

    @Autowired MockMvc mockMvc;
    @Autowired AdminBootstrapService bootstrapService;
    @Autowired UserRepository userRepository;
    @Autowired JwtEncoder jwtEncoder;

    @Test
    void logsInAndReturnsCurrentUserWithBearerToken() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"Login-Password-2026!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("로그인 관리자"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rotatesRefreshTokenAndRejectsReusedTokenFamily() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String first = loginAndGetRefreshToken();

        var refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + first + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        String second = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.refreshToken");

        assertInvalidRefreshToken(first);
        assertInvalidRefreshToken(second);
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotentForUnknownToken() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
        assertInvalidRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown-token\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsSameUnauthorizedErrorForUnknownEmailAndWrongPassword() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");

        assertInvalidCredentials("unknown@example.com", PASSWORD);
        assertInvalidCredentials(EMAIL, "Wrong-Password-2026!");
    }

    @Test
    void locksAccountAfterFiveFailedLoginsAndKeepsGenericError() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertInvalidCredentials(EMAIL, "Wrong-Password-2026!");
        }

        var locked = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(locked.status()).isEqualTo(UserStatus.LOCKED);
        assertThat(locked.failedLoginAttempts()).isEqualTo(5);
        assertThat(locked.lockedAt()).isNotNull();
        assertInvalidCredentials(EMAIL, PASSWORD);
    }

    @Test
    void successfulLoginResetsPreviousFailedAttempts() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        assertInvalidCredentials(EMAIL, "Wrong-Password-2026!");

        loginAndGetRefreshToken();

        var user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.failedLoginAttempts()).isZero();
    }

    @Test
    void rejectsMissingTamperedExpiredAndWrongAudienceTokens() throws Exception {
        mockMvc.perform(get("/api/v1/samples").header("X-Trace-Id", "missing-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "missing-token"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("missing-token"));

        mockMvc.perform(get("/api/v1/samples").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/samples").header("Authorization", "Bearer " + signedToken(
                        Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), "ino-admin-web")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/samples").header("Authorization", "Bearer " + signedToken(
                        Instant.now(), Instant.now().plusSeconds(60), "another-client")))
                .andExpect(status().isUnauthorized());
    }

    private void assertInvalidCredentials(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    private String loginAndGetRefreshToken() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private void assertInvalidRefreshToken(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private String signedToken(Instant issuedAt, Instant expiresAt, String audience) {
        var claims = JwtClaimsSet.builder()
                .issuer("ino-admin")
                .audience(List.of(audience))
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
