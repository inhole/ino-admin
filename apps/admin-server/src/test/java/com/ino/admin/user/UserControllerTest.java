package com.ino.admin.user;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ino.admin.AdminServerApplication;
import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserPage;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import com.ino.admin.identity.api.UserManagementUseCase;
import com.ino.admin.identity.api.RoleCatalogUseCase;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.spring.modules.web.TraceIdFilter;
import java.util.List;
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

@WebMvcTest(UserController.class)
@Import({
        ApplicationConfig.class,
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        UserControllerTest.SecurityBeans.class
})
class UserControllerTest {
    @Autowired MockMvc mockMvc;

    @MockitoBean UserDirectoryUseCase userDirectory;
    @MockitoBean UserManagementUseCase userManagement;
    @MockitoBean RoleCatalogUseCase roleCatalog;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void forwardsTypedUserQueryParameters() throws Exception {
        var expectedQuery = new UserQuery(
                "kim",
                "ADMIN",
                "ACTIVE",
                1,
                10,
                UserSort.DISPLAY_NAME,
                SortDirection.ASC);
        when(userDirectory.findUsers(eq(expectedQuery)))
                .thenReturn(new UserPage(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read")))
                        .queryParam("query", "kim")
                        .queryParam("role", "ADMIN")
                        .queryParam("status", "ACTIVE")
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .queryParam("sort", "displayName")
                        .queryParam("direction", "asc"))
                .andExpect(status().isOk());

        verify(userDirectory).findUsers(expectedQuery);
    }

    @Test
    void userReaderCanReadSafeRoleCatalogWithoutPermissionRead() throws Exception {
        when(roleCatalog.findActiveRoles()).thenReturn(List.of(
                new RoleCatalogUseCase.RoleOption("ADMIN", "관리자"),
                new RoleCatalogUseCase.RoleOption("VIEWER", "조회자")));

        mockMvc.perform(get("/api/v1/users/roles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].displayName").value("관리자"))
                .andExpect(jsonPath("$[0].permissions").doesNotExist());

        verify(roleCatalog).findActiveRoles();
    }

    @Test
    void permissionReaderWithoutUserReadCannotReadRoleCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/users/roles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("permission:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(roleCatalog);
    }

    @Test
    void rejectsUnsupportedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read")))
                        .queryParam("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(userDirectory);
    }

    @Test
    void rejectsUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read")))
                        .queryParam("sort", "passwordHash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(userDirectory);
    }

    @Test
    void rejectsUnsupportedDirection() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read")))
                        .queryParam("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(userDirectory);
    }

    @Test
    void rejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("user:read")))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(userDirectory);
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
