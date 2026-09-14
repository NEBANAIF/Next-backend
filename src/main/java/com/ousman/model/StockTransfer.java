package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A stock transfer between two branches — Warehouse↔Warehouse,
 * Warehouse↔Store, or Store↔Store, since every location's stock lives on
 * a branch either way. Follows a Request → Approve → Complete workflow:
 *
 *   PENDING   — requested, nothing has moved yet
 *   APPROVED  — a manager with access to the source branch approved it;
 *               this is the moment stock actually leaves the source
 *               branch's batches and new batch(es) are created at the
 *               destination, preserving each source batch's original cost
 *               (see StockTransferAllocation / StockTransferService)
 *   REJECTED  — closed without any stock movement
 *   COMPLETED — the destination branch confirms receipt, closing the loop
 *               for accountability; no further stock movement happens here
 */
@Entity
@Table(name = "stock_transfers", indexes = {
    @Index(name = "idx_transfers_from_branch", columnList = "from_branch_id"),
    @Index(name = "idx_transfers_to_branch", columnList = "to_branch_id"),
    @Index(name = "idx_transfers_status", columnList = "status"),
    @Index(name = "idx_transfers_product_id", columnList = "product_id")
})
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_branch_id", nullable = false)
    private Branch fromBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_branch_id", nullable = false)
    private Branch toBranch;

    @Column(nullable = false)
    private Integer quantity;

    // PENDING | APPROVED | REJECTED | COMPLETED
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String status = "PENDING";

    @Column(length = 500)
    private String notes;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ── Request-time selection field (NOT persisted) ──────────────────
    // Optional manual override: if set, this transfer must be fully
    // covered by that one source batch. If null, consumed automatically,
    // oldest-received first (FIFO), same convention as Sale.batchId.
    @Transient
    private Long batchId;

    @PrePersist
    protected void onCreate() {
        if (status == null || status.isBlank()) status = "PENDING";
        requestedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Branch getFromBranch() { return fromBranch; }
    public void setFromBranch(Branch fromBranch) { this.fromBranch = fromBranch; }

    public Branch getToBranch() { return toBranch; }
    public void setToBranch(Branch toBranch) { this.toBranch = toBranch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
