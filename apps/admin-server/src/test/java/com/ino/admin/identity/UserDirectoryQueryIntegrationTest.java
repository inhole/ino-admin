package com.ino.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserDirectoryQueryIntegrationTest {
    private static final Instant BASE_TIME = Instant.parse("2026-08-24T00:00:00Z");

    @Autowired UserDirectoryUseCase userDirectory;
    @Autowired UserRepository userRepository;

    @Test
    void searchesDisplayNameAndEmailCaseInsensitively() {
        save("name-jpa54@example.com", "Kim JPA54", "ADMIN", UserStatus.ACTIVE, BASE_TIME);
        save("email-match-jpa54@example.com", "다른 사용자", "VIEWER", UserStatus.ACTIVE, BASE_TIME);

        var byName = find("KIM JPA54", "", "", 0, 20, UserSort.CREATED_AT, SortDirection.ASC);
        var byEmail = find("EMAIL-MATCH-JPA54", "", "", 0, 20, UserSort.CREATED_AT, SortDirection.ASC);

        assertThat(byName.content()).extracting(UserDirectoryUseCase.UserSummary::email)
                .containsExactly("name-jpa54@example.com");
        assertThat(byEmail.content()).extracting(UserDirectoryUseCase.UserSummary::email)
                .containsExactly("email-match-jpa54@example.com");
    }

    @Test
    void combinesRoleAndStatusWhileReturningAccurateCountAndPage() {
        save("combo-jpa54-a@example.com", "Combo JPA54 A", "ADMIN", UserStatus.ACTIVE, BASE_TIME);
        save("combo-jpa54-b@example.com", "Combo JPA54 B", "ADMIN", UserStatus.DISABLED, BASE_TIME);
        save("combo-jpa54-c@example.com", "Combo JPA54 C", "ADMIN", UserStatus.ACTIVE, BASE_TIME);
        save("combo-jpa54-d@example.com", "Combo JPA54 D", "VIEWER", UserStatus.ACTIVE, BASE_TIME);

        var result = find("combo-jpa54", "ADMIN", "ACTIVE", 1, 1, UserSort.EMAIL, SortDirection.ASC);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).extracting(UserDirectoryUseCase.UserSummary::email)
                .containsExactly("combo-jpa54-c@example.com");
    }

    @ParameterizedTest(name = "sort={0}")
    @MethodSource("sortCases")
    void appliesEveryAllowedSortToTheJpaQuery(UserSort sort, List<String> expectedEmails) {
        save("sort-jpa54-c@example.com", "Bravo JPA54", "VIEWER", UserStatus.ACTIVE,
                BASE_TIME.plusSeconds(3));
        save("sort-jpa54-a@example.com", "Charlie JPA54", "ADMIN", UserStatus.LOCKED,
                BASE_TIME.plusSeconds(2));
        save("sort-jpa54-b@example.com", "Alpha JPA54", "SUPER_ADMIN", UserStatus.DISABLED,
                BASE_TIME.plusSeconds(1));

        var result = find("sort-jpa54", "", "", 0, 20, sort, SortDirection.ASC);

        assertThat(result.content()).extracting(UserDirectoryUseCase.UserSummary::email)
                .containsExactlyElementsOf(expectedEmails);
    }

    @Test
    void breaksEqualPrimarySortValuesByIdAscending() {
        var first = save("tie-jpa54-a@example.com", "Tie JPA54", "ADMIN", UserStatus.ACTIVE, BASE_TIME);
        var second = save("tie-jpa54-b@example.com", "Tie JPA54", "ADMIN", UserStatus.ACTIVE, BASE_TIME);
        var expectedIds = List.of(first.id(), second.id()).stream()
                .sorted(Comparator.comparing(Object::toString))
                .toList();

        var result = find("tie-jpa54", "", "", 0, 20, UserSort.DISPLAY_NAME, SortDirection.DESC);

        assertThat(result.content()).extracting(UserDirectoryUseCase.UserSummary::id)
                .containsExactlyElementsOf(expectedIds);
    }

    static java.util.stream.Stream<Arguments> sortCases() {
        return java.util.stream.Stream.of(
                Arguments.of(UserSort.CREATED_AT, List.of(
                        "sort-jpa54-b@example.com", "sort-jpa54-a@example.com", "sort-jpa54-c@example.com")),
                Arguments.of(UserSort.DISPLAY_NAME, List.of(
                        "sort-jpa54-b@example.com", "sort-jpa54-c@example.com", "sort-jpa54-a@example.com")),
                Arguments.of(UserSort.EMAIL, List.of(
                        "sort-jpa54-a@example.com", "sort-jpa54-b@example.com", "sort-jpa54-c@example.com")),
                Arguments.of(UserSort.ROLE, List.of(
                        "sort-jpa54-a@example.com", "sort-jpa54-b@example.com", "sort-jpa54-c@example.com")),
                Arguments.of(UserSort.STATUS, List.of(
                        "sort-jpa54-c@example.com", "sort-jpa54-b@example.com", "sort-jpa54-a@example.com")));
    }

    private User save(String email, String displayName, String role, UserStatus status, Instant createdAt) {
        var user = User.create(email, "hash", displayName, role, createdAt);
        if (status != UserStatus.ACTIVE) {
            user.changeStatus(status, createdAt);
        }
        return userRepository.saveAndFlush(user);
    }

    private UserDirectoryUseCase.UserPage find(String query, String role, String status, int page, int size,
            UserSort sort, SortDirection direction) {
        return userDirectory.findUsers(new UserQuery(query, role, status, page, size, sort, direction));
    }
}
