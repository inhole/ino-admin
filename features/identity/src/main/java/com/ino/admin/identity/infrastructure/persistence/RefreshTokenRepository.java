package com.ino.admin.identity.infrastructure.persistence;

import com.ino.admin.identity.domain.RefreshToken;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Query("select token.user.id from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Query("select token.familyId from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findFamilyIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.familyId = :familyId order by token.id")
    List<RefreshToken> findAllByFamilyId(@Param("familyId") UUID familyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.user.id = :userId order by token.id")
    List<RefreshToken> findAllByUser_Id(@Param("userId") UUID userId);
}
