package com.ino.admin.identity.infrastructure.persistence;

import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    java.util.List<User> findAllByRole(String role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.role = :role order by user.id")
    java.util.List<User> findAllByRoleForUpdate(@Param("role") String role);

    @Query("select user.id from User user where user.role = :role order by user.id")
    java.util.List<UUID> findIdsByRoleOrderById(@Param("role") String role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from User user
            where user.role = 'SUPER_ADMIN'
              and user.status = com.ino.admin.identity.domain.UserStatus.ACTIVE
            order by user.id
            """)
    java.util.List<User> findAllActiveSuperAdminsForUpdate();

    @Query("""
            select user from User user
            where (:query = ''
               or lower(user.email) like lower(concat('%', :query, '%'))
               or lower(user.displayName) like lower(concat('%', :query, '%')))
              and (:role is null or user.role = :role)
              and (:status is null or user.status = :status)
            """)
    Page<User> search(@Param("query") String query,
                      @Param("role") String role,
                      @Param("status") UserStatus status,
                      Pageable pageable);
}
