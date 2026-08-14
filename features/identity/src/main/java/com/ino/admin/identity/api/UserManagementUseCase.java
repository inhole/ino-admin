package com.ino.admin.identity.api;

import java.util.UUID;

public interface UserManagementUseCase {
    CreatedUser create(CreateUser command);

    record CreateUser(String email, String password, String displayName, String role) {}
    record CreatedUser(UUID id, String email, String displayName, String status, String role) {}
}
