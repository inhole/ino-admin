package com.ino.admin.identity.infrastructure.persistence;

import com.ino.admin.identity.domain.Role;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findAllByOrderByKeyAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select role from Role role where role.key = :roleKey")
    java.util.Optional<Role> findByIdForUpdate(@Param("roleKey") String roleKey);
}
