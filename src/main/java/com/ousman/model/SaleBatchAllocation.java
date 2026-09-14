package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records that a Sale consumed `quantity` units from a specific
 * ProductBatch, at that batch's cost. A single sale can span more than one
 * allocation row if it draws down one batch and spills into the next
 * (e.g. FIFO consumption crossing a batch boundary).
 *
 * This is what makes Sale.costOfGoods accurate per batch cost instead of a
 * single flat Product.cost, and is also what a Return (phase 3) reverses —
 * crediting the exact batch(es) a returned unit came from.
 */
@Entity
@Table(name = "sale_batch_allocations", indexes = {
    @Index(name = "idx_sba_sale_id", columnList = "sale_id"),
    @Index(name = "idx_sba_batch_id", columnList = "batch_id")
})
public class SaleBatchAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductBatch batch;

    @Column(nullable = false)
    private Integer quantity;

    // Snapshot of the batch's cost at the moment of sale — the batch's own
    // cost could theoretically be corrected later, so this is what actually
    // priced this sale and must never silently drift.
    @Column(name = "cost_per_unit", nullable = false)
    private Double costPerUnit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public ProductBatch getBatch() { return batch; }
    public void setBatch(ProductBatch batch) { this.batch = batch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(Double costPerUnit) { this.costPerUnit = costPerUnit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
