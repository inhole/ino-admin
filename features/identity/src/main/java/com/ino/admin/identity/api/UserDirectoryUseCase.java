package com.ino.admin.identity.api;

import com.ino.admin.core.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public interface UserDirectoryUseCase {
    UserPage findUsers(UserQuery query);

    default UserPage findUsers(String query, int page, int size) {
        return findUsers(new UserQuery(query, "", "", page, size, UserSort.CREATED_AT, SortDirection.DESC));
    }

    UserSummary findUser(UUID userId);

    record UserQuery(String query, String role, String status, int page, int size,
                     UserSort sort, SortDirection direction) {}

    record UserSummary(UUID id, String email, String displayName, String status, String role, Instant createdAt) {}
    record UserPage(List<UserSummary> content, int page, int size, long totalElements, int totalPages) {}

    enum UserSort {
        CREATED_AT("createdAt"),
        DISPLAY_NAME("displayName"),
        EMAIL("email"),
        ROLE("role"),
        STATUS("status");

        private final String property;

        UserSort(String property) {
            this.property = property;
        }

        public String property() {
            return property;
        }

        public static UserSort from(String value) {
            if (value == null || value.isBlank()) {
                return CREATED_AT;
            }
            return switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "createdat" -> CREATED_AT;
                case "displayname" -> DISPLAY_NAME;
                case "email" -> EMAIL;
                case "role" -> ROLE;
                case "status" -> STATUS;
                default -> throw new BusinessException("INVALID_USER_SORT", "허용되지 않은 사용자 정렬입니다.");
            };
        }
    }

    enum SortDirection {
        ASC,
        DESC;

        public org.springframework.data.domain.Sort.Direction springDirection() {
            return this == ASC
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC;
        }

        public static SortDirection from(String value) {
            if (value == null || value.isBlank()) {
                return DESC;
            }
            return switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "asc" -> ASC;
                case "desc" -> DESC;
                default -> throw new BusinessException("INVALID_SORT_DIRECTION", "허용되지 않은 정렬 방향입니다.");
            };
        }
    }
}
