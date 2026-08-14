package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ino.admin.identity.domain.Role;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionCatalogServiceTest {
    @Test
    void exposesStableRolePermissionMappings() {
        var repository = mock(RoleRepository.class);
        var superAdmin = mock(Role.class); var admin = mock(Role.class);
        when(superAdmin.key()).thenReturn("SUPER_ADMIN");
        when(superAdmin.permissions()).thenReturn(Set.of("user:read", "user:create", "user:update", "permission:read"));
        when(admin.key()).thenReturn("ADMIN"); when(admin.permissions()).thenReturn(Set.of("user:read"));
        when(repository.findAllByOrderByKeyAsc()).thenReturn(List.of(admin, superAdmin));
        var catalog = new PermissionCatalogService(repository).findAll();

        assertThat(catalog).filteredOn(item -> item.role().equals("SUPER_ADMIN")).singleElement()
                .extracting(item -> item.permissions()).asList()
                .contains("user:read", "user:create", "user:update", "permission:read");
        assertThat(catalog).filteredOn(item -> item.role().equals("ADMIN")).singleElement()
                .extracting(item -> item.permissions()).asList().containsExactly("user:read");
    }
}
