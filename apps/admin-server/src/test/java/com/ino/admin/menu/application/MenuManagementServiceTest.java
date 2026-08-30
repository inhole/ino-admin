package com.ino.admin.menu.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.core.BusinessException;
import com.ino.admin.menu.api.MenuManagementUseCase;
import com.ino.admin.menu.api.MenuManagementUseCase.SaveMenu;
import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuManagementServiceTest {
    private final MenuRepository repository = mock(MenuRepository.class);
    private final MenuManagementService service = new MenuManagementService(repository,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsValidatedMenu() {
        when(repository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(new SaveMenu("reports", null, "보고서", "/reports", "menu", 50,
                "report:read", true));
        assertThat(result.id()).isEqualTo("reports");
        assertThat(result.requiredPermission()).isEqualTo("report:read");
    }

    @Test
    void rejectsDuplicateIdAndSelfParent() {
        when(repository.existsById("duplicate")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new SaveMenu("duplicate", null, "중복", "/d", "menu", 1, null, true)))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("MENU_ID_ALREADY_EXISTS");
        when(repository.existsById("self")).thenReturn(false, true);
        assertThatThrownBy(() -> service.create(new SaveMenu("self", "self", "순환", "/s", "menu", 2, null, true)))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("MENU_CYCLE");
    }

    @Test
    void reordersWholeMenuTreeUpToThreeLevels() {
        var root = new Menu("root", null, "루트", "/root", "menu", 10, null, true);
        var child = new Menu("child", null, "자식", "/child", "menu", 20, null, true);
        var leaf = new Menu("leaf", null, "말단", "/leaf", "menu", 30, null, true);
        when(repository.findAll()).thenReturn(List.of(root, child, leaf));

        var result = service.reorder(List.of(
                new MenuManagementUseCase.MenuPosition("root", null, 10),
                new MenuManagementUseCase.MenuPosition("child", "root", 10),
                new MenuManagementUseCase.MenuPosition("leaf", "child", 10)));

        assertThat(result).extracting(MenuManagementUseCase.MenuView::id)
                .containsExactly("root", "child", "leaf");
        assertThat(child.parentId()).isEqualTo("root");
        assertThat(leaf.parentId()).isEqualTo("child");
        verify(repository, times(2)).flush();
    }

    @Test
    void rejectsFourthLevelAndIncompleteReorderRequests() {
        var menus = List.of(
                new Menu("one", null, "1", "/1", "menu", 10, null, true),
                new Menu("two", "one", "2", "/2", "menu", 10, null, true),
                new Menu("three", "two", "3", "/3", "menu", 10, null, true),
                new Menu("four", "three", "4", "/4", "menu", 10, null, true));
        when(repository.findAll()).thenReturn(menus);

        assertThatThrownBy(() -> service.reorder(List.of(
                new MenuManagementUseCase.MenuPosition("one", null, 10),
                new MenuManagementUseCase.MenuPosition("two", "one", 10),
                new MenuManagementUseCase.MenuPosition("three", "two", 10),
                new MenuManagementUseCase.MenuPosition("four", "three", 10))))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("MENU_DEPTH_EXCEEDED");

        assertThatThrownBy(() -> service.reorder(List.of(
                new MenuManagementUseCase.MenuPosition("one", null, 10))))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("INVALID_MENU_REORDER");
    }

    @Test
    void rejectsCreatingAFourthLevelMenu() {
        when(repository.existsById("four")).thenReturn(false);
        when(repository.existsById("three")).thenReturn(true);
        when(repository.findById("three")).thenReturn(java.util.Optional.of(
                new Menu("three", "two", "3", "/3", "menu", 10, null, true)));
        when(repository.findById("two")).thenReturn(java.util.Optional.of(
                new Menu("two", "one", "2", "/2", "menu", 10, null, true)));

        assertThatThrownBy(() -> service.create(
                new SaveMenu("four", "three", "4", "/4", "menu", 10, null, true)))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("MENU_DEPTH_EXCEEDED");
    }
}
