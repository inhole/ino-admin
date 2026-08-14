package com.ino.admin.menu.infrastructure.persistence;

import com.ino.admin.menu.domain.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, String> {
    List<Menu> findAllByEnabledTrueOrderBySortOrderAsc();
}
