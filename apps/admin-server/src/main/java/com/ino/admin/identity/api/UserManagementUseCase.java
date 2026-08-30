package com.ino.admin.identity.api;

import java.util.UUID;

public interface UserManagementUseCase {
    CreatedUser create(CreateUser command);
    UpdatedUser changeStatus(UUID actorId, UUID userId, String status);
    UpdatedProfile updateProfile(UUID actorId, UUID userId, UpdateProfile command);

    record CreateUser(String email, String password, String displayName, String role) {}
    record CreatedUser(UUID id, String email, String displayName, String status, String role) {}
    record UpdatedUser(UUID id, String status) {}
    record UpdateProfile(String displayName, String role) {}
    record UpdatedProfile(UUID id, String displayName, String role) {}
}
