package com.ino.admin.menu.application;

import com.ino.admin.core.BusinessException;
import com.ino.admin.error.ErrorCode;
import com.ino.admin.menu.api.MenuManagementUseCase;
import com.ino.admin.menu.domain.Menu;
import com.ino.admin.menu.infrastructure.persistence.MenuRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    @Transactional
    public List<MenuView> reorder(List<MenuPosition> positions) {
        var menus = repository.findAll();
        var menusById = new HashMap<String, Menu>();
        menus.forEach(menu -> menusById.put(menu.id(), menu));
        validatePositions(positions, menusById);
        var now = Instant.now(clock);
        for (int index = 0; index < menus.size(); index++) {
            menus.get(index).move(null, 100_000 + index, now);
        }
        repository.flush();
        positions.forEach(position -> menusById.get(position.id())
                .move(blankToNull(position.parentId()), position.order(), now));
        repository.flush();
        return positions.stream().map(position -> view(menusById.get(position.id()))).toList();
    }

    private void validatePositions(List<MenuPosition> positions, Map<String, Menu> menusById) {
        if (positions == null || positions.size() != menusById.size()) throw error(ErrorCode.INVALID_MENU_REORDER);
        var ids = new HashSet<String>();
        var siblingPositions = new HashSet<String>();
        for (var position : positions) {
            if (position == null || !menusById.containsKey(position.id()) || !ids.add(position.id()) || position.order() < 0)
                throw error(ErrorCode.INVALID_MENU_REORDER);
            var parentId = blankToNull(position.parentId());
            if (parentId != null && !menusById.containsKey(parentId)) throw error(ErrorCode.INVALID_MENU_PARENT);
            if (!siblingPositions.add(String.valueOf(parentId) + "\u0000" + position.order()))
                throw error(ErrorCode.MENU_ORDER_DUPLICATED);
        }
        var byId = new HashMap<String, MenuPosition>();
        positions.forEach(position -> byId.put(position.id(), position));
        positions.forEach(position -> validateDepth(position.id(), byId, new HashSet<>(), 1));
    }

    private void validateDepth(String id, Map<String, MenuPosition> positions, Set<String> visited, int depth) {
        if (depth > 3) throw error(ErrorCode.MENU_DEPTH_EXCEEDED);
        if (!visited.add(id)) throw error(ErrorCode.MENU_CYCLE);
        var parentId = blankToNull(positions.get(id).parentId());
        if (parentId != null) validateDepth(parentId, positions, visited, depth + 1);
    }

    private void validate(SaveMenu command, String id) {
        var parentId = blankToNull(command.parentId());
        if (parentId != null && !repository.existsById(parentId)) throw error(ErrorCode.INVALID_MENU_PARENT);
        if (id.equals(parentId)) throw error(ErrorCode.MENU_CYCLE);
        var visited = new HashSet<String>();
        var current = parentId;
        var depth = 1;
        while (current != null) {
            if (++depth > 3) throw error(ErrorCode.MENU_DEPTH_EXCEEDED);
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
