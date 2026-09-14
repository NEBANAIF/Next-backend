package com.ousman.repository;

import com.ousman.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    /** Used by BankService to block deleting a bank that has payment history. */
    boolean existsByBankId(Long bankId);

    boolean existsBySaleId(Long saleId);

    /** Cascade cleanup — called when a sale is deleted (see SaleService.deleteSale). */
    long deleteBySaleId(Long saleId);

    // ── Paginated + searchable + filterable listing (powers the Payments page) ──
    //
    // search matches customer name, cashier, transaction ref, payment code,
    // bank name, or the linked sale's product name — case-insensitive substring.
    // method / bankId / date range are optional filters (pass null to skip).
    //
    // Uses explicit LEFT JOINs so payments with no bank (CASH) or whose sale's
    // product lookup would otherwise force an inner join still show up when
    // those filters aren't in use.
    @Query("SELECT p FROM Payment p " +
           "LEFT JOIN p.bank b " +
           "LEFT JOIN p.sale s " +
           "LEFT JOIN s.product pr " +
           "WHERE (:search IS NULL OR :search = '' " +
           "  OR LOWER(p.customerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(p.cashier) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(p.transactionRef) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(p.paymentCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(pr.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:method IS NULL OR :method = '' OR p.paymentMethod = :method) " +
           "AND (:bankId IS NULL OR b.id = :bankId) " +
           "AND (:from IS NULL OR p.paymentDate >= :from) " +
           "AND (:to IS NULL OR p.paymentDate <= :to)")
    Page<Payment> search(@Param("search") String search,
                          @Param("method") String method,
                          @Param("bankId") Long bankId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.paymentDate = :date")
    Double sumByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.paymentDate BETWEEN :from AND :to")
    Double sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
