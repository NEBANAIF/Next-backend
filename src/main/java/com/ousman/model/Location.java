package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Top-level location — a Store or a Warehouse. A Location never holds
 * stock directly: it always owns one or more Branches, and every batch of
 * stock lives on a Branch (see ProductBatch.branch). Every Location gets an
 * automatic "Main" branch the moment it's created (see BranchService),
 * so a simple single-site store/warehouse still works with zero extra
 * setup, while a location that grows into several sub-sites can add more
 * branches under itself later without changing how anything else works.
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_locations_name", columnList = "name")
})
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    // STORE | WAREHOUSE
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30) DEFAULT 'STORE'")
    private String type = "STORE";

    @Column(length = 250)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(name = "manager_name", length = 150)
    private String managerName;

    @Column(length = 500)
    private String notes;

    // Inactive locations stay in history but are hidden from pickers used
    // when recording new activity (receiving, sales, transfers).
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (type == null || type.isBlank()) type = "STORE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
