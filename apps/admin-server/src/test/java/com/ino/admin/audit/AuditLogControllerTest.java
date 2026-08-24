package com.ino.admin.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ino.admin.AdminServerApplication;
import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.admin.web.TraceIdFilter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@Import({ApplicationConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class,
        AuditLogControllerTest.SecurityBeans.class})
class AuditLogControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuditLogService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void requiresAuditReadPermission() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsFilteredAuditPageForAuthorizedReader() throws Exception {
        var actorId = UUID.randomUUID();
        var command = new AuditCommand(actorId, "USER_UPDATE", "/api/v1/users/1", AuditResult.SUCCESS,
                200, "127.0.0.1", "browser", "trace-1");
        var log = AuditLog.create(command, Instant.parse("2026-08-24T00:00:00Z"));
        when(service.find(eq(actorId), eq("USER_UPDATE"), eq(AuditResult.SUCCESS), any(), any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("audit:read")))
                        .queryParam("actorId", actorId.toString())
                        .queryParam("action", "USER_UPDATE")
                        .queryParam("result", "SUCCESS")
                        .queryParam("createdFrom", "2026-08-01T00:00:00Z")
                        .queryParam("createdTo", "2026-09-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorId").value(actorId.toString()))
                .andExpect(jsonPath("$.content[0].resource").value("/api/v1/users/1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsUnsupportedAction() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("audit:read")))
                        .queryParam("action", "invalid-action"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = AdminServerApplication.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "com\\.ino\\.admin\\.auth\\.(SecurityConfig|RestAuthenticationEntryPoint|RestAccessDeniedHandler)"))
    static class SecurityBeans {}
}
