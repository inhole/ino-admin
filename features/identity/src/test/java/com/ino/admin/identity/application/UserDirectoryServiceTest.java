package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;

class UserDirectoryServiceTest {
    @Test
    void returnsMappedUsersWithNormalizedSearchQuery() {
        var repository = mock(UserRepository.class);
        var createdAt = Instant.parse("2026-08-14T00:00:00Z");
        var user = User.createInitialAdmin("admin@example.com", "hash", "관리자", createdAt);
        when(repository.search(eq("admin"), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(user)));

        var result = new UserDirectoryService(repository).findUsers(new UserQuery(
                " admin ", "", "", 0, 20, UserSort.CREATED_AT, SortDirection.DESC));

        assertThat(result.content()).singleElement().satisfies(summary -> {
            assertThat(summary.email()).isEqualTo("admin@example.com");
            assertThat(summary.displayName()).isEqualTo("관리자");
            assertThat(summary.status()).isEqualTo("ACTIVE");
            assertThat(summary.role()).isEqualTo("SUPER_ADMIN");
            assertThat(summary.createdAt()).isEqualTo(createdAt);
        });
    }

    @Test
    void appliesFiltersAndStableSort() {
        var repository = mock(UserRepository.class);
        when(repository.search(eq("admin"), eq("ADMIN"), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(Page.empty());

        new UserDirectoryService(repository).findUsers(new UserQuery(
                " admin ", "ADMIN", "ACTIVE", 2, 10, UserSort.DISPLAY_NAME, SortDirection.ASC));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq("admin"), eq("ADMIN"), eq(UserStatus.ACTIVE), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().toList())
                .extracting(Sort.Order::getProperty, Sort.Order::getDirection)
                .containsExactly(
                        tuple("displayName", Sort.Direction.ASC),
                        tuple("id", Sort.Direction.ASC));
    }
}
