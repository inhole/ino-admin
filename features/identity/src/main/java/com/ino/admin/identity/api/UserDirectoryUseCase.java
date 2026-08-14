package com.ino.admin.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserDirectoryUseCase {
    UserPage findUsers(String query, int page, int size);

    record UserSummary(UUID id, String email, String displayName, String status, Instant createdAt) {}
    record UserPage(List<UserSummary> content, int page, int size, long totalElements, int totalPages) {}
}
