package com.ousman.service;

import com.ousman.model.Bank;
import com.ousman.model.Payment;
import com.ousman.model.Sale;
import com.ousman.repository.BankRepository;
import com.ousman.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  PaymentService — creates and queries Payment records.
 *
 *  Payments are never created directly from the frontend as a standalone
 *  form; they're always the byproduct of money actually changing hands
 *  against a Sale — either at sale-recording time (SaleService.recordSale)
 *  or on a later loan repayment (SaleService.updateSalePayment). This
 *  keeps the Sale ⇄ Payment relationship guaranteed-consistent instead of
 *  relying on the frontend to keep two separate forms in sync.
 * ─────────────────────────────────────────────────────────────────────────
 */
@Service
@Transactional
public class PaymentService {

    private static final Set<String> VALID_METHODS = Set.of("CASH", "BANK");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BankRepository bankRepository;

    // ── Write: called internally by SaleService ─────────────────────────

    /**
     * Creates a payment record linked to {@code sale} for the given amount.
     * Returns null (creates nothing) when amountPaid is null/zero/negative —
     * a zero-value "payment" isn't a real transaction worth logging.
     *
     * @param paymentMethod "CASH" or "BANK" (case-insensitive); defaults to CASH if blank
     * @param bankId        required when paymentMethod is BANK, ignored otherwise
     * @param transactionRef optional external transaction/reference number
     * @param notes         optional free-text note
     * @param amountPaid    the amount actually collected in this transaction
     * @param cashier       who collected it (recorded-by / logged-in user)
     */
    public Payment createForSale(Sale sale, String paymentMethod, Long bankId,
                                  String transactionRef, String notes,
                                  Double amountPaid, String cashier) {
        if (amountPaid == null || amountPaid <= 0) return null;
        if (sale == null || sale.getId() == null) {
            throw new RuntimeException("Cannot record a payment without a saved sale.");
        }

        String method = (paymentMethod == null || paymentMethod.isBlank())
                ? "CASH" : paymentMethod.trim().toUpperCase();
        if (!VALID_METHODS.contains(method)) {
            throw new RuntimeException("Invalid payment method: '" + paymentMethod + "'. Must be Cash or Bank.");
        }

        Bank bank = null;
        if ("BANK".equals(method)) {
            if (bankId == null) {
                throw new RuntimeException("Please select a bank for a bank payment.");
            }
            bank = bankRepository.findById(bankId)
                    .orElseThrow(() -> new RuntimeException("Selected bank not found."));
            if (Boolean.FALSE.equals(bank.getActive())) {
                throw new RuntimeException("Selected bank is inactive and cannot receive new payments.");
            }
        }

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setCustomerName(sale.getCustomerName());
        payment.setPaymentMethod(method);
        payment.setBank(bank);
        payment.setAmountPaid(amountPaid);
        payment.setCashier((cashier != null && !cashier.isBlank()) ? cashier : "System");
        payment.setTransactionRef((transactionRef != null && !transactionRef.isBlank()) ? transactionRef.trim() : null);
        payment.setNotes((notes != null && !notes.isBlank()) ? notes.trim() : null);

        // Save once to get the generated id, then stamp the human-facing
        // code derived from it and save again.
        Payment saved = paymentRepository.saveAndFlush(payment);
        saved.setPaymentCode("PAY-" + String.format("%06d", saved.getId()));
        return paymentRepository.saveAndFlush(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Payment> search(String search, String method, Long bankId,
                                 LocalDate from, LocalDate to, Pageable pageable) {
        String m = (method == null || method.isBlank()) ? null : method.trim().toUpperCase();
        return paymentRepository.search(search, m, bankId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Payment getByCode(String code) {
        return paymentRepository.findByPaymentCode(code)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + code));
    }

    // ── Write: admin corrections ─────────────────────────────────────────

    /**
     * Admin-only correction — only the transaction reference and notes may
     * be edited after the fact. Amount, method, bank, cashier, timestamps,
     * and the sale link are treated as an immutable financial record.
     */
    public Payment updateNotes(String code, String transactionRef, String notes) {
        Payment p = getByCode(code);
        p.setTransactionRef((transactionRef != null && !transactionRef.isBlank()) ? transactionRef.trim() : null);
        p.setNotes((notes != null && !notes.isBlank()) ? notes.trim() : null);
        return paymentRepository.saveAndFlush(p);
    }

    public void delete(String code) {
        Payment p = getByCode(code);
        paymentRepository.delete(p);
    }

    /**
     * Cascade cleanup — called by SaleService.deleteSale so a deleted sale
     * never leaves orphaned Payment rows behind (Payment.sale is a
     * non-nullable FK, so this must run before the Sale row itself is
     * removed).
     */
    public void deleteAllForSale(Long saleId) {
        paymentRepository.deleteBySaleId(saleId);
    }
}
