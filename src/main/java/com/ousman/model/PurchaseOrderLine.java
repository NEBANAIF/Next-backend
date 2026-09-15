package com.ousman.model;

import jakarta.persistence.*;

/**
 * One product line on a PurchaseOrder. `receivedQuantity` accumulates as
 * receipts come in (a line can be received in more than one delivery);
 * once it equals `orderedQuantity` the line is fully received. Each
 * receipt against this line creates its own ProductBatch at `costPerUnit`
 * — see PurchaseOrderService.receiveLine.
 */
@Entity
@Table(name = "purchase_order_lines", indexes = {
    @Index(name = "idx_pol_po_id", columnList = "purchase_order_id"),
    @Index(name = "idx_pol_product_id", columnList = "product_id")
})
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "received_quantity", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer receivedQuantity = 0;

    @Column(name = "cost_per_unit", nullable = false)
    private Double costPerUnit;

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (receivedQuantity == null) receivedQuantity = 0;
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public Double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(Double costPerUnit) { this.costPerUnit = costPerUnit; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
