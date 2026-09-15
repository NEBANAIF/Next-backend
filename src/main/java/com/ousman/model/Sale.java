package com.ousman.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales", indexes = {
    @Index(name = "idx_sales_sale_date", columnList = "sale_date"),
    @Index(name = "idx_sales_remaining_loan", columnList = "remaining_loan"),
    @Index(name = "idx_sales_product_id", columnList = "product_id"),
    @Index(name = "idx_sales_customer_name", columnList = "customer_name")
})
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Which branch this sale's stock was drawn from. Nullable so existing
    // sales recorded before branches existed remain valid — new sales for
    // a product that already has batches will require this.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // Actual cost of goods sold, computed from the batch(es) this sale drew
    // from (see SaleBatchAllocation) — null when the product has no batch
    // history yet and the sale fell back to the legacy flat-stock path.
    @Column(name = "cost_of_goods")
    private Double costOfGoods;

    @Column(nullable = false)
    private Integer quantity;

    // Selling price per unit at time of sale
    @Column(nullable = false)
    private Double price;

    // Total revenue = price * quantity
    @Column(nullable = false)
    private Double total;

    @Column(name = "customer_name")
    private String customerName;

    // Optional link to a saved Customer record (branch-scoped CRM). Walk-in
    // sales can skip this and just use customerName as free text.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "sale_time")
    private LocalTime saleTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Payment tracking fields ───────────────────────────
    // PAID_FULL = fully paid, PARTIAL_LOAN = customer owes some amount
    @Column(name = "payment_status", nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'PAID_FULL'")
    private String paymentStatus = "PAID_FULL";

    // How much the customer has paid so far
    @Column(name = "paid_amount", nullable = false, columnDefinition = "FLOAT DEFAULT 0.0")
    private Double paidAmount = 0.0;

    // Remaining unpaid balance (total - paidAmount)
    @Column(name = "remaining_loan", nullable = false, columnDefinition = "FLOAT DEFAULT 0.0")
    private Double remainingLoan = 0.0;

    // ── Payment-integration request fields (NOT persisted on this table) ──
    // These ride along on the sale-creation request body so the frontend can
    // submit sale + payment details in a single call. SaleService reads them
    // to create the linked Payment record (see PaymentService), then they're
    // discarded — the actual payment method / bank selection lives on the
    // Payment row, not here, keeping Sales, Payments, and Banks normalized.
    @Transient
    private String paymentMethod;   // "CASH" or "BANK" — defaults to CASH

    @Transient
    private Long bankId;            // required when paymentMethod = BANK

    @Transient
    private String transactionRef;  // optional external transaction/reference number

    @Transient
    private String paymentNotes;    // optional note attached to the payment record

    // ── Batch-selection request field (NOT persisted — see costOfGoods) ──
    // Optional manual override: if set, this sale must be fully covered by
    // that one batch (error if it doesn't have enough remaining). If left
    // null, SaleService consumes batches automatically, oldest-received
    // first (FIFO), possibly spanning more than one batch.
    @Transient
    private Long batchId;

    @PrePersist
    protected void onCreate() {
        if (saleDate == null) saleDate = LocalDate.now();
        if (saleTime == null) saleTime = LocalTime.now();
        if (total == null && price != null && quantity != null) {
            total = price * quantity;
        }
        createdAt = LocalDateTime.now();
        // Default payment status if not set
        if (paymentStatus == null) paymentStatus = "PAID_FULL";
        if (paidAmount == null) paidAmount = 0.0;
        if (remainingLoan == null) remainingLoan = 0.0;
    }

    // ── Getters & Setters ────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Double getCostOfGoods() { return costOfGoods; }
    public void setCostOfGoods(Double costOfGoods) { this.costOfGoods = costOfGoods; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public LocalTime getSaleTime() { return saleTime; }
    public void setSaleTime(LocalTime saleTime) { this.saleTime = saleTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }

    public Double getRemainingLoan() { return remainingLoan; }
    public void setRemainingLoan(Double remainingLoan) { this.remainingLoan = remainingLoan; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getPaymentNotes() { return paymentNotes; }
    public void setPaymentNotes(String paymentNotes) { this.paymentNotes = paymentNotes; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
