package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Grants a user access to one branch. Only enforced for the location-scoped
 * roles (WAREHOUSE_MANAGER, STORE_MANAGER, STAFF) — see AccessControlService.
 * ADMIN always sees everything regardless of any rows here, and the legacy
 * WORKER role keeps its original system-wide visibility so existing worker
 * accounts created before branches existed keep working unchanged.
 */
@Entity
@Table(name = "user_branch_access",
    uniqueConstraints = @UniqueConstraint(name = "uq_user_branch", columnNames = {"user_id", "branch_id"}),
    indexes = {
        @Index(name = "idx_uba_user_id", columnList = "user_id"),
        @Index(name = "idx_uba_branch_id", columnList = "branch_id")
    })
public class UserBranchAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
