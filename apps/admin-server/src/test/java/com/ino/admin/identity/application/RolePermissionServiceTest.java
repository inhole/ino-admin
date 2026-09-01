package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ino.spring.modules.core.BusinessException;
import com.ino.admin.identity.domain.Role;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RolePermissionServiceTest {
    private final RoleRepository repository = mock(RoleRepository.class);
    private final RolePermissionService service = new RolePermissionService(repository);

    @Test
    void returnsSortedPermissions() {
        var role = mock(Role.class);
        when(role.enabled()).thenReturn(true);
        when(role.permissions()).thenReturn(Set.of("user:update", "user:read"));
        when(repository.findById("ADMIN")).thenReturn(Optional.of(role));
        assertThat(service.findPermissions("ADMIN")).containsExactly("user:read", "user:update");
    }

    @Test
    void returnsNoPermissionsForDisabledRole() {
        var role = mock(Role.class);
        when(role.enabled()).thenReturn(false);
        when(role.permissions()).thenReturn(Set.of("user:read", "user:update"));
        when(repository.findById("DISABLED_ADMIN")).thenReturn(Optional.of(role));

        assertThat(service.findPermissions("DISABLED_ADMIN")).isEmpty();
    }

    @Test
    void rejectsUnknownRole() {
        when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findPermissions("UNKNOWN"))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("ROLE_NOT_FOUND");
    }
}
