package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.UserManagementUseCase.CreateUser;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import com.ino.admin.identity.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserManagementServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserManagementService service = new UserManagementService(repository, encoder,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC), refreshTokenService, roleRepository);

    @Test
    void createsViewerWithNormalizedEmailAndEncodedPassword() {
        when(encoder.encode("Viewer-Password-2026!")).thenReturn("encoded");
        var role = mock(Role.class); when(role.enabled()).thenReturn(true);
        when(roleRepository.findByIdForUpdate("VIEWER")).thenReturn(Optional.of(role));

        var result = service.create(new CreateUser(" Viewer@Example.com ", "Viewer-Password-2026!", " 뷰어 ", "VIEWER"));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(result.email()).isEqualTo("viewer@example.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().role()).isEqualTo("VIEWER");
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
        var user = User.create("viewer@example.com", "hash", "뷰어", "VIEWER",
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
    void rejectsDisablingLastActiveSuperAdmin() {
        var superAdmin = User.createInitialAdmin("last-admin@example.com", "hash", "마지막 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findById(superAdmin.id())).thenReturn(Optional.of(superAdmin));
        when(repository.findAllActiveSuperAdminsForUpdate()).thenReturn(List.of(superAdmin));

        assertThatThrownBy(() -> service.changeStatus(UUID.randomUUID(), superAdmin.id(), "DISABLED"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("LAST_SUPER_ADMIN_PROTECTED");
    }

    @Test
    void allowsDisablingSuperAdminWhenAnotherActiveSuperAdminRemains() {
        var target = User.createInitialAdmin("target-admin@example.com", "hash", "대상 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        var remaining = User.createInitialAdmin("remaining-admin@example.com", "hash", "남은 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findById(target.id())).thenReturn(Optional.of(target));
        when(repository.findAllActiveSuperAdminsForUpdate()).thenReturn(List.of(target, remaining));

        var result = service.changeStatus(UUID.randomUUID(), target.id(), "DISABLED");

        assertThat(result.status()).isEqualTo("DISABLED");
    }

    @Test
    void updatesAnotherUsersProfileAndRevokesRefreshTokens() {
        var user = User.create("viewer@example.com", "hash", "뷰어", "VIEWER",
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findByIdForUpdate(user.id())).thenReturn(Optional.of(user));
        var role = mock(Role.class); when(role.enabled()).thenReturn(true);
        when(roleRepository.findByIdForUpdate("ADMIN")).thenReturn(Optional.of(role));

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

    @Test
    void rejectsChangingLastActiveSuperAdminRole() {
        var superAdmin = User.createInitialAdmin("last-admin@example.com", "hash", "마지막 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findByIdForUpdate(superAdmin.id())).thenReturn(Optional.of(superAdmin));
        when(repository.findAllActiveSuperAdminsForUpdate()).thenReturn(List.of(superAdmin));
        var role = mock(Role.class);
        when(role.enabled()).thenReturn(true);
        when(roleRepository.findByIdForUpdate("ADMIN")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.updateProfile(UUID.randomUUID(), superAdmin.id(),
                new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile("관리자", "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("LAST_SUPER_ADMIN_PROTECTED");
    }

    @Test
    void allowsChangingSuperAdminRoleWhenAnotherActiveSuperAdminRemains() {
        var target = User.createInitialAdmin("target-admin@example.com", "hash", "대상 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        var remaining = User.createInitialAdmin("remaining-admin@example.com", "hash", "남은 최고 관리자",
                Instant.parse("2026-08-13T00:00:00Z"));
        when(repository.findAllActiveSuperAdminsForUpdate()).thenReturn(List.of(target, remaining));
        when(repository.findByIdForUpdate(target.id())).thenReturn(Optional.of(target));
        var role = mock(Role.class);
        when(role.enabled()).thenReturn(true);
        when(roleRepository.findByIdForUpdate("ADMIN")).thenReturn(Optional.of(role));

        var result = service.updateProfile(UUID.randomUUID(), target.id(),
                new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile("관리자", "ADMIN"));

        assertThat(result.role()).isEqualTo("ADMIN");
    }
}
