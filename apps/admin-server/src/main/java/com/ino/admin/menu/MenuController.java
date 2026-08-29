package com.ino.admin.menu;

import com.ino.admin.menu.api.MenuQueryUseCase;
import com.ino.admin.menu.api.MenuManagementUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
class MenuController {
    private final MenuQueryUseCase menuQuery;
    private final MenuManagementUseCase menuManagement;

    MenuController(MenuQueryUseCase menuQuery, MenuManagementUseCase menuManagement) {
        this.menuQuery = menuQuery;
        this.menuManagement = menuManagement;
    }

    @GetMapping
    List<MenuManagementUseCase.MenuView> findAll() { return menuManagement.findAll(); }

    @PostMapping
    MenuManagementUseCase.MenuView create(@Valid @RequestBody SaveMenuRequest request) {
        return menuManagement.create(request.toCommand());
    }

    @PatchMapping("/{id}")
    MenuManagementUseCase.MenuView update(@PathVariable String id, @Valid @RequestBody SaveMenuRequest request) {
        return menuManagement.update(id, request.toCommand());
    }

    @PatchMapping("/order")
    List<MenuManagementUseCase.MenuView> reorder(@Valid @RequestBody List<ReorderMenuRequest> request) {
        return menuManagement.reorder(request.stream().map(ReorderMenuRequest::toCommand).toList());
    }

    @GetMapping("/me")
    List<MenuQueryUseCase.MenuItem> findMine(@AuthenticationPrincipal Jwt jwt) {
        return menuQuery.findAccessibleMenus(Set.copyOf(
                java.util.Optional.ofNullable(jwt.getClaimAsStringList("permissions")).orElse(List.of())));
    }

    record SaveMenuRequest(@NotBlank @Size(max = 50) String id, @Size(max = 50) String parentId,
            @NotBlank @Size(max = 100) String label, @NotBlank @Size(max = 255) String route,
            @NotBlank @Size(max = 50) String icon, @Min(0) int order,
            @Size(max = 100) String requiredPermission, boolean enabled) {
        MenuManagementUseCase.SaveMenu toCommand() {
            return new MenuManagementUseCase.SaveMenu(id, parentId, label, route, icon, order, requiredPermission, enabled);
        }
    }


    record ReorderMenuRequest(@NotBlank @Size(max = 50) String id, @Size(max = 50) String parentId,
            @Min(0) int order) {
        MenuManagementUseCase.MenuPosition toCommand() {
            return new MenuManagementUseCase.MenuPosition(id, parentId, order);
        }
    }
}
