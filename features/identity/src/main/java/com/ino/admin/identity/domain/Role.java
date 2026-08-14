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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_key"))
    @Column(name = "permission_key")
    private Set<String> permissions = new LinkedHashSet<>();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {}

    public String key() { return key; }
    public String displayName() { return displayName; }
    public boolean systemRole() { return systemRole; }
    public Set<String> permissions() { return Set.copyOf(permissions); }

    public void replacePermissions(Set<String> newPermissions, Instant now) {
        permissions.clear();
        permissions.addAll(newPermissions);
        updatedAt = now;
    }
}
