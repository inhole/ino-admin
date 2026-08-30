package com.ino.admin.security.jwt;

import java.time.Clock;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@AutoConfiguration
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class JwtSecurityAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    SecretKey jwtSecretKey(JwtSecurityProperties properties) {
        validateText(properties.getIssuer(), "APP_JWT_ISSUER 설정이 필요합니다.");
        validateText(properties.getAudience(), "APP_JWT_AUDIENCE 설정이 필요합니다.");
        if (properties.getAccessTokenTtl() == null || properties.getAccessTokenTtl().isNegative()
                || properties.getAccessTokenTtl().isZero()) {
            throw new IllegalStateException("APP_JWT_ACCESS_TOKEN_TTL은 0보다 커야 합니다.");
        }
        validateText(properties.getSecret(), "APP_JWT_SECRET 설정이 필요합니다.");
        final byte[] decoded;
        try { decoded = Base64.getDecoder().decode(properties.getSecret()); }
        catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_JWT_SECRET은 Base64 형식이어야 합니다.", exception);
        }
        if (decoded.length < 32) throw new IllegalStateException("APP_JWT_SECRET은 디코딩 기준 32바이트 이상이어야 합니다.");
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean @ConditionalOnMissingBean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(SecretKey secretKey, JwtSecurityProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<>("aud",
                claim -> claim instanceof List<?> values && values.contains(properties.getAudience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    @Bean @ConditionalOnMissingBean
    JwtTokenService jwtTokenService(JwtEncoder encoder, JwtSecurityProperties properties, Clock clock) {
        return new JwtTokenService(encoder, properties, clock);
    }

    @Bean @ConditionalOnMissingBean
    JwtPermissionAuthenticationConverter jwtPermissionAuthenticationConverter() {
        return new JwtPermissionAuthenticationConverter();
    }

    private static void validateText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
    }
}
