package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A bank the store can receive payments through (e.g. "CBE — Awash Branch").
 * Selected on the Sales form whenever a sale's payment method is BANK, and
 * referenced (by name only, never by id) from Payment records.
 */
@Entity
@Table(name = "banks", indexes = {
    @Index(name = "idx_banks_name", columnList = "name")
})
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(length = 150)
    private String branch;

    @Column(length = 500)
    private String notes;

    // Inactive banks stay in history (old payments still reference them)
    // but are hidden from the picker on the Sales form.
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
