package com.ino.admin.monitoring;

import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonitoringController.class)
@Import({
        ApplicationConfig.class,
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        MonitoringControllerTest.SecurityBeans.class
})
class MonitoringControllerTest {
    @Autowired MockMvc mockMvc;

    @MockitoBean MonitoringSummaryService monitoringSummaryService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void returnsStableNullableMonitoringSummaryForAuthorizedReader() throws Exception {
        when(monitoringSummaryService.getSummary()).thenReturn(new MonitoringSummary(
                Instant.parse("2026-08-24T00:00:00Z"),
                0.42d,
                null,
                1024d,
                4096d,
                3600d,
                12d,
                18d,
                3L,
                0.625d,
                1L));

        mockMvc.perform(get("/api/v1/monitoring/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("monitoring:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value("2026-08-24T00:00:00Z"))
                .andExpect(jsonPath("$.systemCpuUsage").value(0.42d))
                .andExpect(jsonPath("$.processCpuUsage").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.heapUsedBytes").value(1024d))
                .andExpect(jsonPath("$.heapMaxBytes").value(4096d))
                .andExpect(jsonPath("$.processUptimeSeconds").value(3600d))
                .andExpect(jsonPath("$.liveThreads").value(12d))
                .andExpect(jsonPath("$.peakThreads").value(18d))
                .andExpect(jsonPath("$.httpRequestCount").value(3))
                .andExpect(jsonPath("$.httpRequestDurationSeconds").value(0.625d))
                .andExpect(jsonPath("$.httpServerErrorCount").value(1));

        verify(monitoringSummaryService).getSummary();
    }

    @Test
    void forbidsCallerWithoutMonitoringReadPermission() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/summary").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @TestConfiguration
    @ComponentScan(
            basePackageClasses = AdminServerApplication.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.ino\\.admin\\.auth\\.(SecurityConfig|RestAuthenticationEntryPoint|RestAccessDeniedHandler)"))
    static class SecurityBeans {}
}
