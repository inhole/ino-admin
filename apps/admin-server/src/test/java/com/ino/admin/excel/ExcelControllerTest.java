package com.ino.admin.excel;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ino.admin.AdminServerApplication;
import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.admin.web.TraceIdFilter;
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

@WebMvcTest(ExcelController.class)
@Import({ApplicationConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class,
        ExcelControllerTest.SecurityBeans.class})
class ExcelControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserExcelExporter exporter;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void exportsXlsxForAuthorizedCaller() throws Exception {
        when(exporter.export()).thenReturn(new byte[] { 1, 2, 3 });
        mockMvc.perform(get("/api/v1/excel/users/export")
                        .with(jwt().authorities(new SimpleGrantedAuthority("excel:export"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void rejectsCallerWithoutExportPermission() throws Exception {
        mockMvc.perform(get("/api/v1/excel/users/export").with(jwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(exporter);
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = AdminServerApplication.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "com\\.ino\\.admin\\.auth\\.(SecurityConfig|RestAuthenticationEntryPoint|RestAccessDeniedHandler)"))
    static class SecurityBeans {}
}
