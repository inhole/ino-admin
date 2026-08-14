package com.ino.admin.menu.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ino.admin.core.BusinessException;
import com.ino.admin.menu.api.MenuManagementUseCase.SaveMenu;
import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
}
