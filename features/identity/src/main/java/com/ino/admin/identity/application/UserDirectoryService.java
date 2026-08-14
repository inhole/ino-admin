package com.ino.admin.identity.application;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
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
    public UserPage findUsers(String query, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var users = userRepository.search(query == null ? "" : query.strip(), pageable);
        var content = users.getContent().stream()
                .map(user -> new UserSummary(user.id(), user.email(), user.displayName(),
                        user.status().name(), user.role().name(), user.createdAt()))
                .toList();
        return new UserPage(content, users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary findUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
        return new UserSummary(user.id(), user.email(), user.displayName(), user.status().name(),
                user.role().name(), user.createdAt());
    }
}
