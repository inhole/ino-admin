package com.ino.admin.identity.domain;

public enum Permission {
    USER_READ("user:read"),
    USER_CREATE("user:create"),
    USER_UPDATE("user:update"),
    PERMISSION_READ("permission:read"),
    MENU_READ("menu:read"),
    MENU_UPDATE("menu:update"),
    PERMISSION_UPDATE("permission:update"),
    FILE_READ("file:read"),
    FILE_WRITE("file:write"),
    MONITORING_READ("monitoring:read"),
    AUDIT_READ("audit:read"),
    EXCEL_EXPORT("excel:export");

    private final String key;

    Permission(String key) { this.key = key; }

    public String key() { return key; }
}
