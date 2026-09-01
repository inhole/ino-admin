package com.ino.admin.identity.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @Column(name = "role_key")
    private String key;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "system_role", nullable = false)
    private boolean systemRole;
    @Column(nullable = false)
    private boolean enabled;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_key"))
    @Column(name = "permission_key")
    private Set<String> permissions = new LinkedHashSet<>();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Role() {}

    public static Role create(String key, String displayName, Set<String> permissions, Instant now) {
        var role = new Role(); role.key = key; role.displayName = displayName.strip(); role.systemRole = false;
        role.enabled = true; role.permissions.addAll(permissions); role.createdAt = now; role.updatedAt = now; return role;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public boolean systemRole() { return systemRole; }
    public boolean enabled() { return enabled; }
    public Set<String> permissions() { return Set.copyOf(permissions); }

    public void replacePermissions(Set<String> newPermissions, Instant now) {
        permissions.clear();
        permissions.addAll(newPermissions);
        updatedAt = now;
    }

    public void rename(String name, Instant now) { displayName = name.strip(); updatedAt = now; }
    public void changeEnabled(boolean value, Instant now) { enabled = value; updatedAt = now; }
}
