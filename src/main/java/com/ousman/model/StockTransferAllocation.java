package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records that an approved StockTransfer drew `quantity` units from a
 * specific source batch, and which new batch that quantity became at the
 * destination branch (created at approval time, carrying over the exact
 * same cost per unit — a transfer never changes what stock actually cost).
 * A transfer can span more than one source batch if FIFO consumption
 * crosses a batch boundary, just like SaleBatchAllocation.
 */
@Entity
@Table(name = "stock_transfer_allocations", indexes = {
    @Index(name = "idx_sta_transfer_id", columnList = "transfer_id"),
    @Index(name = "idx_sta_source_batch_id", columnList = "source_batch_id"),
    @Index(name = "idx_sta_destination_batch_id", columnList = "destination_batch_id")
})
public class StockTransferAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_batch_id", nullable = false)
    private ProductBatch sourceBatch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_batch_id", nullable = false)
    private ProductBatch destinationBatch;

    @Column(nullable = false)
    private Integer quantity;

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

    public StockTransfer getTransfer() { return transfer; }
    public void setTransfer(StockTransfer transfer) { this.transfer = transfer; }

    public ProductBatch getSourceBatch() { return sourceBatch; }
    public void setSourceBatch(ProductBatch sourceBatch) { this.sourceBatch = sourceBatch; }

    public ProductBatch getDestinationBatch() { return destinationBatch; }
    public void setDestinationBatch(ProductBatch destinationBatch) { this.destinationBatch = destinationBatch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(Double costPerUnit) { this.costPerUnit = costPerUnit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
