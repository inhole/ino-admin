package com.ino.admin.menu.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.menu.api.MenuManagementUseCase;
import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuManagementService implements MenuManagementUseCase {
    private final MenuRepository repository;
    private final Clock clock;

    public MenuManagementService(MenuRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuView> findAll() { return repository.findAllByOrderBySortOrderAsc().stream().map(this::view).toList(); }

    @Override
    @Transactional
    public MenuView create(SaveMenu command) {
        if (repository.existsById(command.id())) throw error("MENU_ID_ALREADY_EXISTS", "이미 존재하는 메뉴 ID입니다.");
        validate(command, command.id());
        var menu = new Menu(command.id().strip(), blankToNull(command.parentId()), command.label().strip(),
                command.route().strip(), command.icon().strip(), command.order(), blankToNull(command.requiredPermission()),
                command.enabled(), Instant.now(clock));
        return view(repository.save(menu));
    }

    @Override
    @Transactional
    public MenuView update(String id, SaveMenu command) {
        var menu = repository.findById(id).orElseThrow(() -> error("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다."));
        validate(command, id);
        menu.update(blankToNull(command.parentId()), command.label(), command.route(), command.icon(), command.order(),
                command.requiredPermission(), command.enabled(), Instant.now(clock));
        return view(menu);
    }

    private void validate(SaveMenu command, String id) {
        var parentId = blankToNull(command.parentId());
        if (parentId != null && !repository.existsById(parentId)) throw error("INVALID_MENU_PARENT", "부모 메뉴가 존재하지 않습니다.");
        if (id.equals(parentId)) throw error("MENU_CYCLE", "메뉴는 자기 자신을 부모로 지정할 수 없습니다.");
        var visited = new HashSet<String>();
        var current = parentId;
        while (current != null) {
            if (!visited.add(current) || id.equals(current)) throw error("MENU_CYCLE", "메뉴 tree에 순환이 생깁니다.");
            current = repository.findById(current).map(Menu::parentId).orElse(null);
        }
        if (repository.existsByParentIdAndSortOrderAndIdNot(parentId, command.order(), id))
            throw error("MENU_ORDER_DUPLICATED", "같은 부모 아래 정렬 순서가 중복됩니다.");
    }

    private MenuView view(Menu menu) { return new MenuView(menu.id(), menu.parentId(), menu.label(), menu.route(), menu.icon(), menu.sortOrder(), menu.requiredPermission(), menu.enabled()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private BusinessException error(String code, String message) { return new BusinessException(code, message); }
}
