package com.ino.admin.identity.infrastructure.persistence;

import com.ino.admin.identity.domain.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findAllByOrderByKeyAsc();
}
