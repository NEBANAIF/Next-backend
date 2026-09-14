package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A customer, always attached to exactly one branch — customers, like
 * everything else operational in this system, don't cross branch
 * boundaries. A scoped-role user only ever sees their own branch's
 * customers; ADMIN sees all of them, filterable by branch.
 *
 * This is a standalone directory for now (contact info + notes) — it is
 * NOT yet linked to Sale by a foreign key. Sale.customerName stays a free
 * text field; a future pass could add an optional Sale.customer FK that,
 * when set, denormalizes into customerName the way it already works today.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_branch_id", columnList = "branch_id"),
    @Index(name = "idx_customers_name", columnList = "name")
})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 250)
    private String address;

    @Column(length = 1000)
    private String notes;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
