package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class UserDirectoryServiceTest {
    @Test
    void returnsMappedUsersWithNormalizedSearchQuery() {
        var repository = mock(UserRepository.class);
        var createdAt = Instant.parse("2026-08-14T00:00:00Z");
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", createdAt);
        when(repository.search(eq("admin"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(user)));

        var result = new UserDirectoryService(repository).findUsers(" admin ", 0, 20);

        assertThat(result.content()).singleElement().satisfies(summary -> {
            assertThat(summary.email()).isEqualTo("admin@example.com");
            assertThat(summary.displayName()).isEqualTo("관리자");
            assertThat(summary.status()).isEqualTo("ACTIVE");
            assertThat(summary.role()).isEqualTo("SUPER_ADMIN");
            assertThat(summary.createdAt()).isEqualTo(createdAt);
        });
    }
}
