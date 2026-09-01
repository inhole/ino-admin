package com.ino.admin.identity.api;

import java.util.UUID;

public interface PasswordChangeUseCase {
    void change(UUID userId, String currentPassword, String newPassword);
}
