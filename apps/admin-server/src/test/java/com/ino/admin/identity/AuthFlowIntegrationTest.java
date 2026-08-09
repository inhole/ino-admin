package com.ino.admin.identity;

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
    void returnsSameUnauthorizedErrorForUnknownEmailAndWrongPassword() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");

        assertInvalidCredentials("unknown@example.com", PASSWORD);
        assertInvalidCredentials(EMAIL, "Wrong-Password-2026!");
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
