package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A branch — the actual stock-holding unit. Every branch belongs to
 * exactly one Location (a Store or a Warehouse) and every batch of stock,
 * sale, receipt, and transfer always targets a specific Branch — never the
 * Location itself. This is what keeps "stock never mixes between
 * locations" strictly true: two branches never share inventory, even if
 * they sit under the same Location.
 *
 * Every Location gets one automatic branch named "Main" the moment it's
 * created (isDefault = true), so a single-site store/warehouse works with
 * zero extra setup. Admins can add more branches under the same Location
 * later (e.g. a store that opens a second counter, or a warehouse with two
 * physically separate sheds) without anything else in the system changing.
 */
@Entity
@Table(name = "branches", indexes = {
    @Index(name = "idx_branches_name", columnList = "name"),
    @Index(name = "idx_branches_location_id", columnList = "location_id")
})
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 250)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(name = "manager_name", length = 150)
    private String managerName;

    @Column(length = 500)
    private String notes;

    // The auto-created branch every Location gets on creation. Kept so the
    // UI can label it clearly and so it can't be deleted out from under a
    // Location that has no other branch to hold its stock.
    @Column(name = "is_default", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefault = false;

    // Inactive branches stay in history but are hidden from pickers used
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
        if (isDefault == null) isDefault = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
