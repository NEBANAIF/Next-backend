package com.ousman.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Payment — one row per amount actually collected against a Sale.
 *
 *  Created automatically:
 *    • when a sale is recorded (for whatever is paid at that moment), and
 *    • when a loan/partial sale receives a later repayment.
 *
 *  Internally linked to Sale (sale_id FK) and optionally to Bank (bank_id
 *  FK), but neither raw id is ever serialized to JSON — callers only ever
 *  see the human-facing paymentCode / saleCode / bankName. The `id`, `sale`
 *  and `bank` fields exist purely for JPA persistence and internal service
 *  logic.
 * ─────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_sale_id",       columnList = "sale_id"),
    @Index(name = "idx_payments_payment_date",  columnList = "payment_date"),
    @Index(name = "idx_payments_customer_name", columnList = "customer_name"),
    @Index(name = "idx_payments_method",        columnList = "payment_method"),
    @Index(name = "idx_payments_bank_id",       columnList = "bank_id"),
    @Index(name = "idx_payments_code",          columnList = "payment_code")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-facing identifier exposed to the API instead of the raw
    // database id, e.g. "PAY-000123". Assigned right after insert once
    // the row has a real id to derive it from — so this column can't be
    // NOT NULL at the DB level (it's briefly null between the first save
    // and the follow-up save that stamps it); PaymentService guarantees
    // every row ends the transaction with a code set before anything else
    // can read it.
    @Column(name = "payment_code", unique = true, length = 30)
    private String paymentCode;

    // Internal FK — which sale this payment was collected against.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    // CASH or BANK
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    // Only set when paymentMethod = BANK
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_time", nullable = false)
    private LocalTime paymentTime;

    // Who collected the payment (cashier / worker / admin email)
    @Column(nullable = false, length = 150)
    private String cashier;

    // Optional external transaction / reference number (e.g. bank slip no.)
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (paymentDate == null) paymentDate = LocalDate.now();
        if (paymentTime == null) paymentTime = LocalTime.now();
        createdAt = LocalDateTime.now();
    }

    // ── Derived, read-only fields exposed to the API ───────────────────
    // Everything the frontend needs to display and search payments,
    // without ever seeing sale_id or bank_id.
    public String getSaleCode() {
        return (sale != null && sale.getId() != null) ? "SALE-" + String.format("%06d", sale.getId()) : null;
    }

    public String getSaleProductName() {
        return (sale != null && sale.getProduct() != null) ? sale.getProduct().getName() : null;
    }

    public Double getSaleTotal() {
        return sale != null ? sale.getTotal() : null;
    }

    public String getBankName() {
        return bank != null ? bank.getName() : null;
    }

    public String getBankBranch() {
        return bank != null ? bank.getBranch() : null;
    }

    // ── Getters & Setters ───────────────────────────────────────────────
    // id, sale, and bank are hidden from JSON — internal use only.
    @JsonIgnore
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    @JsonIgnore
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @JsonIgnore
    public Bank getBank() { return bank; }
    public void setBank(Bank bank) { this.bank = bank; }

    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalTime paymentTime) { this.paymentTime = paymentTime; }

    public String getCashier() { return cashier; }
    public void setCashier(String cashier) { this.cashier = cashier; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
