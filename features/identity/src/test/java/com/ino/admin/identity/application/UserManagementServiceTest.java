package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.UserManagementUseCase.CreateUser;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserRole;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserManagementServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final UserManagementService service = new UserManagementService(repository, encoder,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC), refreshTokenService);

    @Test
    void createsViewerWithNormalizedEmailAndEncodedPassword() {
        when(encoder.encode("Viewer-Password-2026!")).thenReturn("encoded");

        var result = service.create(new CreateUser(" Viewer@Example.com ", "Viewer-Password-2026!", " 뷰어 ", "VIEWER"));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(result.email()).isEqualTo("viewer@example.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().role()).isEqualTo(UserRole.VIEWER);
    }

    @Test
    void rejectsDuplicateEmailAndSuperAdminAssignment() {
        when(repository.existsByEmail("used@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateUser("used@example.com", "Valid-Password-2026!", "중복", "VIEWER")))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThatThrownBy(() -> service.create(new CreateUser("new@example.com", "Valid-Password-2026!", "최고 관리자", "SUPER_ADMIN")))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("INVALID_USER_ROLE");
    }

    @Test
    void disablesAnotherUserAndRevokesRefreshTokens() {
        var actorId = UUID.randomUUID();
        var user = User.create("viewer@example.com", "hash", "뷰어", UserRole.VIEWER,
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findById(user.id())).thenReturn(Optional.of(user));

        var result = service.changeStatus(actorId, user.id(), "DISABLED");

        assertThat(result.status()).isEqualTo("DISABLED");
        verify(refreshTokenService).revokeAllForUser(user.id());
    }

    @Test
    void rejectsSelfDisable() {
        var actorId = UUID.randomUUID();
        assertThatThrownBy(() -> service.changeStatus(actorId, actorId, "DISABLED"))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("SELF_DISABLE_NOT_ALLOWED");
    }

    @Test
    void updatesAnotherUsersProfileAndRevokesRefreshTokens() {
        var user = User.create("viewer@example.com", "hash", "뷰어", UserRole.VIEWER,
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findById(user.id())).thenReturn(Optional.of(user));

        var result = service.updateProfile(UUID.randomUUID(), user.id(),
                new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile(" 운영자 ", "ADMIN"));

        assertThat(result.displayName()).isEqualTo("운영자");
        assertThat(result.role()).isEqualTo("ADMIN");
        verify(refreshTokenService).revokeAllForUser(user.id());
    }

    @Test
    void rejectsSelfRoleChange() {
        var actorId = UUID.randomUUID();
        assertThatThrownBy(() -> service.updateProfile(actorId, actorId,
                new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile("관리자", "ADMIN")))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("SELF_ROLE_CHANGE_NOT_ALLOWED");
    }
}
