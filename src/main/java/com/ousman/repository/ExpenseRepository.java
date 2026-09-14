package com.ousman.repository;

import com.ousman.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Expense repository — all date-part queries use EXTRACT()
 * which is PostgreSQL / Neon compatible.
 * Never uses MySQL-specific YEAR() / MONTH() functions.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    /** All expenses ordered newest first */
    @Query("SELECT e FROM Expense e ORDER BY e.date DESC, e.createdAt DESC")
    List<Expense> findAllOrdered();

    /** All expenses in a date range */
    List<Expense> findByDateBetween(LocalDate from, LocalDate to);

    /** All expenses for a specific category */
    List<Expense> findByCategory(String category);

    // ── Aggregates ────────────────────────────────────────────────────────────

    /**
     * Total expense amount in a date range.
     * Returns 0 when no expenses exist — never returns null thanks to COALESCE.
     * Called by AnalyticsService to compute net profit.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.date BETWEEN :from AND :to")
    Double sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ── Analytics: daily series ───────────────────────────────────────────────

    /**
     * Sum of expenses grouped by date — used to build the daily expense series.
     * Returns List of Object[]{LocalDate date, Double totalAmount}.
     */
    @Query("SELECT e.date, COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e WHERE e.date BETWEEN :from AND :to " +
           "GROUP BY e.date ORDER BY e.date")
    List<Object[]> sumByExpenseDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ── Analytics: monthly series — EXTRACT is PostgreSQL / Neon safe ─────────

    /**
     * Sum of expenses grouped by year and month.
     * Uses EXTRACT(YEAR/MONTH FROM ...) — works on PostgreSQL and Neon.
     * Returns List of Object[]{Double year, Double month, Double totalAmount}.
     */
    @Query("SELECT EXTRACT(YEAR FROM e.date), EXTRACT(MONTH FROM e.date), " +
           "COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e WHERE e.date BETWEEN :from AND :to " +
           "GROUP BY EXTRACT(YEAR FROM e.date), EXTRACT(MONTH FROM e.date) " +
           "ORDER BY EXTRACT(YEAR FROM e.date), EXTRACT(MONTH FROM e.date)")
    List<Object[]> sumExpenseByYearMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);
}