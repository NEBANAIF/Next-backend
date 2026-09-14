package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A supplier — global, like the product catalog, not branch-scoped. A
 * supplier does business with the whole company, and the same supplier is
 * routinely used to fulfill purchase orders for more than one branch, so
 * splitting this per-branch wouldn't reflect how purchasing actually works.
 * Every PurchaseOrder references one; branch scoping happens on the order
 * itself (which branch is receiving the stock), not on the supplier.
 */
@Entity
@Table(name = "suppliers", indexes = {
    @Index(name = "idx_suppliers_name", columnList = "name")
})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

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
