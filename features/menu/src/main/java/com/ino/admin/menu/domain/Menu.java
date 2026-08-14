package com.ino.admin.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected Menu() {}

    public Menu(String id, String parentId, String label, String route, String icon, int sortOrder,
            String requiredPermission, boolean enabled) {
        this.id = id;
        this.parentId = parentId;
        this.label = label;
        this.route = route;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.requiredPermission = requiredPermission;
        this.enabled = enabled;
    }

    public String id() { return id; }
    public String parentId() { return parentId; }
    public String label() { return label; }
    public String route() { return route; }
    public String icon() { return icon; }
    public int sortOrder() { return sortOrder; }
    public String requiredPermission() { return requiredPermission; }
}
