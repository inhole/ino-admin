package com.ino.admin.security.jwt;

import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class JwtPermissionAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var permissions = java.util.Optional.ofNullable(jwt.getClaimAsStringList("permissions")).orElse(List.of());
        return new JwtAuthenticationToken(jwt, permissions.stream().map(SimpleGrantedAuthority::new).toList());
    }
}
