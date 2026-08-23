package com.ino.admin.identity.application;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDirectoryService implements UserDirectoryUseCase {
    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPage findUsers(UserQuery query) {
        var normalizedQuery = normalizeQuery(query.query());
        var normalizedRole = normalizeOptional(query.role());
        var status = parseStatus(query.status());
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(query.direction().springDirection(), query.sort().property())
                        .and(Sort.by(Sort.Direction.ASC, "id")));
        var users = userRepository.search(normalizedQuery, normalizedRole, status, pageable);
        var content = users.getContent().stream()
                .map(user -> new UserSummary(user.id(), user.email(), user.displayName(),
                        user.status().name(), user.role(), user.createdAt()))
                .toList();
        return new UserPage(content, users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary findUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        return new UserSummary(user.id(), user.email(), user.displayName(), user.status().name(),
                user.role(), user.createdAt());
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private UserStatus parseStatus(String status) {
        var normalized = normalizeOptional(status);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> UserStatus.ACTIVE;
            case "LOCKED" -> UserStatus.LOCKED;
            case "DISABLED" -> UserStatus.DISABLED;
            default -> throw new BusinessException("INVALID_USER_STATUS", "허용되지 않은 사용자 상태입니다.");
        };
    }
}
