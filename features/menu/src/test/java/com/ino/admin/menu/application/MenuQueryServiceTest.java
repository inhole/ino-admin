package com.ino.admin.menu.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MenuQueryServiceTest {
    private final MenuQueryService service = new MenuQueryService();

    @Test
    void filtersAndOrdersMenusByPermission() {
        assertThat(service.findAccessibleMenus(Set.of("user:read")))
                .extracting(item -> item.id()).containsExactly("dashboard", "users");
    }

    @Test
    void alwaysIncludesDashboard() {
        assertThat(service.findAccessibleMenus(Set.of()))
                .extracting(item -> item.id()).containsExactly("dashboard");
    }
}
