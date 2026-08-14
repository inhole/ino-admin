package com.ino.admin.identity.domain;

import java.util.EnumSet;
import java.util.Set;

public final class RolePermissions {
    private RolePermissions() {}

    public static Set<Permission> forRole(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> EnumSet.allOf(Permission.class);
            case ADMIN -> EnumSet.of(Permission.USER_READ);
            case VIEWER -> EnumSet.noneOf(Permission.class);
        };
    }
}
