package com.ousman.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password; // BCrypt hashed — accepted on create/update, never returned

    // ADMIN | WORKER
    @Column(nullable = false)
    private String role = "WORKER";

    // ACTIVE | INACTIVE
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ── Branch-assignment request field (NOT persisted here) ─────────────
    // Required at creation time for the three location-scoped roles
    // (WAREHOUSE_MANAGER, STORE_MANAGER, STAFF) — see UserService.create.
    // The actual assignment lives in UserBranchAccess rows, not on this
    // entity, so a user can later be granted access to more than one
    // branch without a schema change. ADMIN and the legacy WORKER role are
    // exempt — they keep unscoped, branch-independent visibility.
    @jakarta.persistence.Transient
    private java.util.List<Long> branchIds;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public java.util.List<Long> getBranchIds() { return branchIds; }
    public void setBranchIds(java.util.List<Long> branchIds) { this.branchIds = branchIds; }
}