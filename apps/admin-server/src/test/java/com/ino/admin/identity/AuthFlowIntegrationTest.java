package com.ino.admin.identity;

import com.ino.admin.identity.bootstrap.AdminBootstrapService;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void listsUsersWithoutExposingPasswordData() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var loginResult = login(PASSWORD);
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/users")
                        .queryParam("query", "login")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.content[0].displayName").value("로그인 관리자"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void viewerCannotListUsers() throws Exception {
        var viewerToken = signedToken(Instant.now(), Instant.now().plusSeconds(60), "ino-admin-web", "VIEWER");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void protectsMonitoringGetRequestsWithMonitoringReadPermission() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String superAdminToken = JsonPath.read(login(PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        createUser(superAdminToken, "monitoring-admin@example.com", "Monitoring-Admin-Password-2026!", "ADMIN");
        createUser(superAdminToken, "monitoring-viewer@example.com", "Monitoring-Viewer-Password-2026!", "VIEWER");

        String viewerToken = JsonPath.read(login("monitoring-viewer@example.com", "Monitoring-Viewer-Password-2026!")
                .getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/monitoring/summary").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String adminToken = JsonPath.read(login("monitoring-admin@example.com", "Monitoring-Admin-Password-2026!")
                .getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/monitoring/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/monitoring/summary").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminReadsPermissionCatalogButViewerCannot() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String token = JsonPath.read(login(PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.role == 'SUPER_ADMIN')].permissions").isNotEmpty());

        var viewerToken = signedToken(Instant.now(), Instant.now().plusSeconds(60), "ino-admin-web", "VIEWER");
        mockMvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsMenusFilteredByTokenPermissions() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String token = JsonPath.read(login(PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/menus/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains(
                        "dashboard", "users", "permissions", "menu-management", "files", "audit-logs")));

        var viewerToken = signedToken(Instant.now(), Instant.now().plusSeconds(60), "ino-admin-web", "VIEWER");
        mockMvc.perform(get("/api/v1/menus/me").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains("dashboard")));
    }

    @Test
    void superAdminCreatesUserButViewerCannot() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var loginResult = login(PASSWORD);
        String superAdminToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        var request = """
                {"email":"viewer@example.com","password":"Viewer-Password-2026!","displayName":"조회 사용자","role":"VIEWER"}
                """;

        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("viewer@example.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var viewerToken = signedToken(Instant.now(), Instant.now().plusSeconds(60), "ino-admin-web", "VIEWER");
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(request.replace("viewer@example.com", "other@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void superAdminDisablesUserButCannotDisableSelf() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var loginResult = login(PASSWORD);
        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        var created = mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"status@example.com","password":"Status-Password-2026!","displayName":"상태 사용자","role":"VIEWER"}
                                """))
                .andExpect(status().isCreated()).andReturn();
        String userId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}/status", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        var actor = userRepository.findByEmail(EMAIL).orElseThrow();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}/status", actor.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_DISABLE_NOT_ALLOWED"));
    }

    @Test
    void anotherActorCannotDisableOrDemoteLastActiveSuperAdmin() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var lastSuperAdmin = userRepository.findByEmail(EMAIL).orElseThrow();
        var otherActorToken = signedToken(Instant.now(), Instant.now().plusSeconds(60), "ino-admin-web", "SUPER_ADMIN");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}/status", lastSuperAdmin.id())
                        .header("Authorization", "Bearer " + otherActorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LAST_SUPER_ADMIN_PROTECTED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}", lastSuperAdmin.id())
                        .header("Authorization", "Bearer " + otherActorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"관리자\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LAST_SUPER_ADMIN_PROTECTED"));
    }

    @Test
    void superAdminReadsAndUpdatesAnotherUserButCannotChangeSelfRole() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String token = JsonPath.read(login(PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        var created = mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"profile@example.com","password":"Profile-Password-2026!","displayName":"프로필 사용자","role":"VIEWER"}
                                """))
                .andExpect(status().isCreated()).andReturn();
        String userId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/users/{userId}", userId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("profile@example.com"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}", userId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"운영 관리자\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("운영 관리자"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        var actor = userRepository.findByEmail(EMAIL).orElseThrow();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/users/{userId}", actor.id()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"관리자\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE_NOT_ALLOWED"));
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
    void disabledRoleCannotRegainPermissionsByLoginOrRefresh() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        String superAdminToken = JsonPath.read(login(PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/permissions/roles")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"CUSTOM_ADMIN","displayName":"커스텀 관리자","permissions":["user:read"]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"custom@example.com","password":"Custom-Password-2026!","displayName":"커스텀 사용자","role":"CUSTOM_ADMIN"}
                                """))
                .andExpect(status().isCreated());

        var activeLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"custom@example.com","password":"Custom-Password-2026!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String activeAccessToken = JsonPath.read(activeLogin.getResponse().getContentAsString(), "$.accessToken");
        String activeRefreshToken = JsonPath.read(activeLogin.getResponse().getContentAsString(), "$.refreshToken");
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + activeAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/permissions/roles/CUSTOM_ADMIN/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertInvalidRefreshToken(activeRefreshToken);

        assertInvalidCredentials("custom@example.com", "Custom-Password-2026!");
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
    void changesPasswordAndRevokesAllRefreshTokens() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var firstLogin = login(PASSWORD);
        var secondLogin = login(PASSWORD);
        String accessToken = JsonPath.read(firstLogin.getResponse().getContentAsString(), "$.accessToken");
        String firstRefresh = JsonPath.read(firstLogin.getResponse().getContentAsString(), "$.refreshToken");
        String secondRefresh = JsonPath.read(secondLogin.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Login-Password-2026!","newPassword":"Changed-Password-2026!"}
                                """))
                .andExpect(status().isOk());

        assertInvalidRefreshToken(firstRefresh);
        assertInvalidRefreshToken(secondRefresh);
        assertInvalidCredentials(EMAIL, PASSWORD);
        login("Changed-Password-2026!");
    }

    @Test
    void rejectsWrongCurrentReusedAndWeakPasswords() throws Exception {
        bootstrapService.bootstrap(EMAIL, PASSWORD, "로그인 관리자");
        var login = login(PASSWORD);
        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        assertPasswordChangeError(accessToken, "Wrong-Password-2026!", "Changed-Password-2026!", "INVALID_CURRENT_PASSWORD");
        assertPasswordChangeError(accessToken, PASSWORD, PASSWORD, "PASSWORD_REUSE_NOT_ALLOWED");
        assertPasswordChangeError(accessToken, PASSWORD, "password-without-required-classes", "PASSWORD_POLICY_VIOLATION");
        login(PASSWORD);
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
        var result = login(PASSWORD);
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private org.springframework.test.web.servlet.MvcResult login(String password) throws Exception {
        return login(EMAIL, password);
    }

    private org.springframework.test.web.servlet.MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void createUser(String accessToken, String email, String password, String role) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"displayName\":\"관제 사용자\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated());
    }

    private void assertPasswordChangeError(String accessToken, String currentPassword, String newPassword, String code)
            throws Exception {
        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
    }

    private void assertInvalidRefreshToken(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private String signedToken(Instant issuedAt, Instant expiresAt, String audience) {
        return signedToken(issuedAt, expiresAt, audience, "VIEWER");
    }

    private String signedToken(Instant issuedAt, Instant expiresAt, String audience, String role) {
        return signedToken(issuedAt, expiresAt, audience, role, role.equals("SUPER_ADMIN")
                ? java.util.Arrays.stream(com.ino.admin.identity.domain.Permission.values())
                        .map(permission -> permission.key()).toList()
                : role.equals("ADMIN") ? List.of("user:read") : List.of());
    }

    private String signedToken(Instant issuedAt, Instant expiresAt, String audience, String role, List<String> permissions) {
        var claims = JwtClaimsSet.builder()
                .issuer("ino-admin")
                .audience(List.of(audience))
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", role)
                .claim("permissions", permissions)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

}
