package com.ino.admin.auth;

import com.ino.admin.security.jwt.JwtPermissionAuthenticationConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            ObjectProvider<JwtPermissionAuthenticationConverter> jwtAuthenticationConverterProvider)
            throws Exception {
        var jwtAuthenticationConverter = jwtAuthenticationConverterProvider
                .getIfAvailable(JwtPermissionAuthenticationConverter::new);
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/users/**")
                                .hasAuthority("user:read")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/users/**")
                                .hasAuthority("user:create")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/users/**")
                                .hasAuthority("user:update")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/permissions/**")
                                .hasAuthority("permission:read")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/permissions/**")
                                .hasAuthority("permission:update")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/permissions/**")
                                .hasAuthority("permission:update")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/menus")
                                .hasAuthority("menu:read")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/menus")
                                .hasAuthority("menu:update")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/menus/**")
                                .hasAuthority("menu:update")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/files/**")
                                .hasAuthority("file:read")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/files/**")
                                .hasAuthority("file:write")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/files/**")
                                .hasAuthority("file:write")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/monitoring/**")
                                .hasAuthority("monitoring:read")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/access-history/**")
                                .hasAuthority("access-history:read")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/excel/users/export")
                                .hasAuthority("excel:export")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/excel/users/import-template")
                                .hasAuthority("excel:import")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/excel/users/import")
                                .hasAuthority("excel:import")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }
}
