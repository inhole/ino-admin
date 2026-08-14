package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.domain.Role;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleManagementServiceTest {
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final RoleManagementService service = new RoleManagementService(roles, users, refreshTokens,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void replacesKnownPermissions() {
        var role = mock(Role.class);
        when(role.key()).thenReturn("ADMIN"); when(role.permissions()).thenReturn(Set.of("menu:read", "user:read"));
        when(roles.findById("ADMIN")).thenReturn(Optional.of(role));
        var result = service.replacePermissions("ADMIN", List.of("user:read", "menu:read"));
        assertThat(result.permissions()).containsExactly("menu:read", "user:read");
        verify(role).replacePermissions(Set.of("user:read", "menu:read"), Instant.parse("2026-08-14T00:00:00Z"));
    }

    @Test
    void protectsSuperAdminAndRejectsUnknownPermission() {
        assertThatThrownBy(() -> service.replacePermissions("SUPER_ADMIN", List.of()))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("SYSTEM_ROLE_PROTECTED");
        when(roles.findById("ADMIN")).thenReturn(Optional.of(mock(Role.class)));
        assertThatThrownBy(() -> service.replacePermissions("ADMIN", List.of("unknown:permission")))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("INVALID_PERMISSION");
    }

    @Test
    void createsCustomRoleAndProtectsSystemRoleLifecycle() {
        when(roles.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create(new com.ino.admin.identity.api.RoleManagementUseCase.CreateRole(
                "content_editor", "콘텐츠 편집자", List.of("user:read")));
        assertThat(created.role()).isEqualTo("CONTENT_EDITOR");
        assertThat(created.enabled()).isTrue();

        var systemRole = mock(Role.class); when(systemRole.systemRole()).thenReturn(true);
        when(roles.findById("ADMIN")).thenReturn(Optional.of(systemRole));
        assertThatThrownBy(() -> service.changeEnabled("ADMIN", false))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("SYSTEM_ROLE_PROTECTED");
    }
}
