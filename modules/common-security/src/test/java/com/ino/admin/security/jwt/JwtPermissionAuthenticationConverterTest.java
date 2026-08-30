package com.ino.admin.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtPermissionAuthenticationConverterTest {
    @Test
    void mapsOnlyPermissionClaimsToAuthorities() {
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-id")
                .claim("role", "ADMIN").claim("permissions", List.of("user:read", "file:write")).build();

        var authentication = new JwtPermissionAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly("user:read", "file:write");
    }
}
