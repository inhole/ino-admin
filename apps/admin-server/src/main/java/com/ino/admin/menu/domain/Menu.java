package com.ino.admin.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "menus")
public class Menu {
    @Id
    private String id;
    @Column(name = "parent_id")
    private String parentId;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false)
    private String route;
    @Column(nullable = false)
    private String icon;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "required_permission")
    private String requiredPermission;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Menu() {}

    public Menu(String id, String parentId, String label, String route, String icon, int sortOrder,
            String requiredPermission, boolean enabled) {
        this(id, parentId, label, route, icon, sortOrder, requiredPermission, enabled, Instant.EPOCH);
    }

    public Menu(String id, String parentId, String label, String route, String icon, int sortOrder,
            String requiredPermission, boolean enabled, Instant now) {
        this.id = id;
        this.parentId = parentId;
        this.label = label;
        this.route = route;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.requiredPermission = requiredPermission;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String id() { return id; }
    public String parentId() { return parentId; }
    public String label() { return label; }
    public String route() { return route; }
    public String icon() { return icon; }
    public int sortOrder() { return sortOrder; }
    public String requiredPermission() { return requiredPermission; }
    public boolean enabled() { return enabled; }

    public void update(String parentId, String label, String route, String icon, int sortOrder,
            String requiredPermission, boolean enabled, Instant now) {
        this.parentId = parentId;
        this.label = label.strip();
        this.route = route.strip();
        this.icon = icon.strip();
        this.sortOrder = sortOrder;
        this.requiredPermission = requiredPermission == null || requiredPermission.isBlank() ? null : requiredPermission.strip();
        this.enabled = enabled;
        this.updatedAt = now;
    }

    public void move(String parentId, int sortOrder, Instant now) {
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
    }
}
