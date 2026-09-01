package com.ino.admin.menu.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MenuQueryServiceTest {
    private final MenuRepository repository = mock(MenuRepository.class);
    private final MenuQueryService service = new MenuQueryService(repository);

    private void catalog() {
        when(repository.findAllByEnabledTrueOrderBySortOrderAsc()).thenReturn(List.of(
                new Menu("dashboard", null, "대시보드", "/", "layout-dashboard", 10, null, true),
                new Menu("users", null, "사용자", "/users", "users", 20, "user:read", true),
                new Menu("permissions", null, "권한", "/permissions", "key-round", 30, "permission:read", true)));
    }

    @Test
    void filtersAndOrdersMenusByPermission() {
        catalog();
        assertThat(service.findAccessibleMenus(Set.of("user:read")))
                .extracting(item -> item.id()).containsExactly("dashboard", "users");
    }

    @Test
    void alwaysIncludesDashboard() {
        catalog();
        assertThat(service.findAccessibleMenus(Set.of()))
                .extracting(item -> item.id()).containsExactly("dashboard");
    }

    @Test
    void rejectsMissingParent() {
        when(repository.findAllByEnabledTrueOrderBySortOrderAsc()).thenReturn(List.of(
                new Menu("orphan", "missing", "고아", "/orphan", "file", 10, null, true)));
        assertThatThrownBy(() -> service.findAccessibleMenus(Set.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("부모");
    }

    @Test
    void rejectsCycleWithoutRoot() {
        when(repository.findAllByEnabledTrueOrderBySortOrderAsc()).thenReturn(List.of(
                new Menu("a", "b", "A", "/a", "file", 10, null, true),
                new Menu("b", "a", "B", "/b", "file", 20, null, true)));
        assertThatThrownBy(() -> service.findAccessibleMenus(Set.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("순환");
    }
}
