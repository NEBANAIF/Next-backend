package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A return against a specific past sale. Always tied to a sale — a
 * standalone stock write-off with no originating sale is a plain
 * adjustment (StockHistory ADJUSTMENT type), not a Return.
 *
 * `restock = true` credits the exact batch(es) the sale drew from back to
 * sellable stock (see BatchService restoration logic) — the item is fine
 * and goes back on the shelf. `restock = false` means the item is damaged/
 * defective and does not return to stock; the Return record still exists
 * for refund/reporting purposes, it just causes no stock movement.
 */
@Entity
@Table(name = "returns", indexes = {
    @Index(name = "idx_returns_sale_id", columnList = "sale_id"),
    @Index(name = "idx_returns_branch_id", columnList = "branch_id")
})
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Denormalized off the sale for fast branch-scoped queries even if the
    // sale itself had no branch (legacy quick sale) — see ReturnService.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean restock = true;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (restock == null) restock = true;
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getRestock() { return restock; }
    public void setRestock(Boolean restock) { this.restock = restock; }

    public Double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Double refundAmount) { this.refundAmount = refundAmount; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
