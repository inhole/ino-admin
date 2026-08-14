package com.ino.admin.menu;

import com.ino.admin.menu.api.MenuQueryUseCase;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
class MenuController {
    private final MenuQueryUseCase menuQuery;

    MenuController(MenuQueryUseCase menuQuery) { this.menuQuery = menuQuery; }

    @GetMapping("/me")
    List<MenuQueryUseCase.MenuItem> findMine(@AuthenticationPrincipal Jwt jwt) {
        return menuQuery.findAccessibleMenus(Set.copyOf(
                java.util.Optional.ofNullable(jwt.getClaimAsStringList("permissions")).orElse(List.of())));
    }
}
