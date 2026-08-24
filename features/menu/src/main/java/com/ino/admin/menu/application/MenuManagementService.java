package com.ino.admin.menu.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
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
        if (repository.existsById(command.id())) throw error(ErrorCode.MENU_ID_ALREADY_EXISTS);
        validate(command, command.id());
        var menu = new Menu(command.id().strip(), blankToNull(command.parentId()), command.label().strip(),
                command.route().strip(), command.icon().strip(), command.order(), blankToNull(command.requiredPermission()),
                command.enabled(), Instant.now(clock));
        return view(repository.save(menu));
    }

    @Override
    @Transactional
    public MenuView update(String id, SaveMenu command) {
        var menu = repository.findById(id).orElseThrow(() -> error(ErrorCode.MENU_NOT_FOUND));
        validate(command, id);
        menu.update(blankToNull(command.parentId()), command.label(), command.route(), command.icon(), command.order(),
                command.requiredPermission(), command.enabled(), Instant.now(clock));
        return view(menu);
    }

    private void validate(SaveMenu command, String id) {
        var parentId = blankToNull(command.parentId());
        if (parentId != null && !repository.existsById(parentId)) throw error(ErrorCode.INVALID_MENU_PARENT);
        if (id.equals(parentId)) throw error(ErrorCode.MENU_CYCLE);
        var visited = new HashSet<String>();
        var current = parentId;
        while (current != null) {
            if (!visited.add(current) || id.equals(current)) throw error(ErrorCode.MENU_CYCLE);
            current = repository.findById(current).map(Menu::parentId).orElse(null);
        }
        if (repository.existsByParentIdAndSortOrderAndIdNot(parentId, command.order(), id))
            throw error(ErrorCode.MENU_ORDER_DUPLICATED);
    }

    private MenuView view(Menu menu) { return new MenuView(menu.id(), menu.parentId(), menu.label(), menu.route(), menu.icon(), menu.sortOrder(), menu.requiredPermission(), menu.enabled()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private BusinessException error(ErrorCode errorCode) { return new BusinessException(errorCode); }
}
