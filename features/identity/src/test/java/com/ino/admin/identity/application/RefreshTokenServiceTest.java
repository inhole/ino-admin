package com.ino.admin.identity.application;

import com.ino.admin.identity.api.InvalidRefreshTokenException;
import com.ino.admin.identity.application.port.AccessTokenIssuer;
import com.ino.admin.identity.domain.RefreshToken;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.infrastructure.persistence.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
    private final RefreshTokenService service = new RefreshTokenService(
            repository, accessTokenIssuer, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(30));

    @Test
    void storesOnlyHashWhenIssuingRefreshToken() throws Exception {
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", NOW);

        var issued = service.issue(user);

        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        var expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(issued.rawToken().getBytes(StandardCharsets.UTF_8)));
        assertThat(captor.getValue().tokenHash()).isEqualTo(expectedHash).doesNotContain(issued.rawToken());
    }

    @Test
    void revokesWholeFamilyWhenRotatedTokenIsReused() {
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", NOW);
        var familyId = UUID.randomUUID();
        var reused = RefreshToken.issue(user, "reused-hash", familyId, NOW, NOW.plusSeconds(60));
        var sibling = RefreshToken.issue(user, "sibling-hash", familyId, NOW, NOW.plusSeconds(60));
        reused.revoke(NOW.minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(reused));
        when(repository.findAllByFamilyId(familyId)).thenReturn(List.of(reused, sibling));

        assertThatThrownBy(() -> service.rotate("reused-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(sibling.isRevoked()).isTrue();
    }
}
