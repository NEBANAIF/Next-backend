package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A batch (lot) of a product received into a specific branch, at a
 * specific unit cost. The same product can have many open batches — at the
 * same or different branches — each with a different cost, because that's
 * what actually happened when it was bought (price changes over time,
 * different suppliers, etc.). A batch carries no selling price and no
 * expiry date — it exists purely to track incoming cost and quantity per
 * branch; selling price lives on Product, and expiry tracking isn't part
 * of this system.
 *
 * `quantityRemaining` is decremented as sales/discards/transfers consume
 * this batch (see SaleBatchAllocation) and is what stock-on-hand is
 * actually computed from, per branch. `quantityReceived` never changes
 * after creation — it's the original lot size, kept for audit/reporting.
 */
@Entity
@Table(name = "product_batches", indexes = {
    @Index(name = "idx_batches_product_id", columnList = "product_id"),
    @Index(name = "idx_batches_branch_id", columnList = "branch_id"),
    @Index(name = "idx_batches_received_date", columnList = "received_date")
})
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Lot reference — optional, purely informational (not used for lookups)
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "cost_per_unit", nullable = false)
    private Double costPerUnit;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "quantity_remaining", nullable = false)
    private Integer quantityRemaining;

    // Which supplier this batch came from — optional (a manual/ad-hoc batch
    // may not have one), set automatically when the batch was created by
    // receiving against a PurchaseOrderLine.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // The purchase order (and specific line) this batch was received
    // against, if any. Null for a manual/ad-hoc batch entry.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_line_id")
    private PurchaseOrderLine purchaseOrderLine;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (receivedDate == null) receivedDate = LocalDate.now();
        if (quantityRemaining == null) quantityRemaining = quantityReceived;
    }

    /** True once every unit of this batch has been sold, discarded, or transferred out. */
    @Transient
    public boolean isDepleted() {
        return quantityRemaining != null && quantityRemaining <= 0;
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(Double costPerUnit) { this.costPerUnit = costPerUnit; }

    public Integer getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(Integer quantityReceived) { this.quantityReceived = quantityReceived; }

    public Integer getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(Integer quantityRemaining) { this.quantityRemaining = quantityRemaining; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public PurchaseOrderLine getPurchaseOrderLine() { return purchaseOrderLine; }
    public void setPurchaseOrderLine(PurchaseOrderLine purchaseOrderLine) { this.purchaseOrderLine = purchaseOrderLine; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
