package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionCatalogServiceTest {
    @Test
    void exposesStableRolePermissionMappings() {
        var catalog = new PermissionCatalogService().findAll();

        assertThat(catalog).filteredOn(item -> item.role().equals("SUPER_ADMIN")).singleElement()
                .extracting(item -> item.permissions()).asList()
                .contains("user:read", "user:create", "user:update", "permission:read");
        assertThat(catalog).filteredOn(item -> item.role().equals("ADMIN")).singleElement()
                .extracting(item -> item.permissions()).asList().containsExactly("user:read");
    }
}
