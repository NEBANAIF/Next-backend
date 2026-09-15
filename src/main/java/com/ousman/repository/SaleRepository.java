package com.ousman.repository;

import com.ousman.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Sale repository — all analytics queries use JPQL with EXTRACT()
 * which is PostgreSQL / Neon compatible.
 * Never uses MySQL-specific YEAR() / MONTH() functions.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    /** All sales on a specific date */
    List<Sale> findBySaleDate(LocalDate date);

    /** All sales on a specific date, for one branch — used by getToday() for branch users. */
    List<Sale> findBySaleDateAndBranchId(LocalDate date, Long branchId);

    /** All sales in a date range — used by hourly series loader */
    List<Sale> findBySaleDateBetween(LocalDate from, LocalDate to);

    /** All sales for a specific product */
    List<Sale> findByProductId(Long productId);

    /** All sales ordered newest first */
    @Query("SELECT s FROM Sale s ORDER BY s.saleDate DESC, s.saleTime DESC")
    List<Sale> findAllOrderedByDate();

    // ── Paginated + searchable listing (powers the Sales page table) ───────────
    //
    // findAllOrderedByDate() above loads EVERY row into memory every time —
    // fine at dozens of sales, gets slow and wasteful once the table has
    // thousands+. This query instead asks the database for exactly one
    // page of rows (LIMIT/OFFSET under the hood via Pageable), so response
    // time and payload size stay flat as the table grows.
    //
    // search matches customer name OR product name, case-insensitive,
    // substring match. Pass null/blank to skip the search filter.
    // NOTE: this is plain substring matching (not the smarter word-boundary
    // "Nebil vs Nebila" matching used on some other pages) — good enough at
    // moderate scale, but the same collision the frontend fixed elsewhere
    // (searching "nebil" also matching "nebila") can still happen here.
    // Worth porting that same word-boundary logic into SQL if this list
    // grows large enough for it to matter in practice.
    //
    // date filters to a single sale date. Pass null to skip.
    @Query("SELECT s FROM Sale s WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(s.customerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:date IS NULL OR s.saleDate = :date) " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Page<Sale> search(@Param("search") String search, @Param("date") LocalDate date,
                       @Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE (:branchId IS NULL OR s.branch.id = :branchId) ORDER BY s.saleDate DESC, s.saleTime DESC")
    List<Sale> findAllOrderedByDate(@Param("branchId") Long branchId);

    // ── Simple aggregates ─────────────────────────────────────────────────────

    /** Total revenue on a single date */
    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.saleDate = :date")
    Double sumRevenueByDate(@Param("date") LocalDate date);

    /** Total revenue in a date range */
    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to")
    Double sumRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Number of sale rows on a single date */
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate = :date")
    Long countByDate(@Param("date") LocalDate date);

    // ── Analytics: totals in date range ──────────────────────────────────────

    /**
     * Total revenue (SUM of s.total) between from and to inclusive.
     * Returns 0 when no sales exist — never returns null thanks to COALESCE.
     */
    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Double sumTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    /**
     * Total units sold (SUM of s.quantity) between from and to inclusive.
     * Returns 0 when no sales exist.
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Long sumQuantityBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    /**
     * Number of sale transactions between from and to inclusive.
     */
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Long countSalesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    /**
     * Cost of Goods Sold = SUM(product.cost × quantity) for all sales in range.
     * Uses the cost stored on the linked Product entity.
     * Returns 0 when no sales exist.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(s.costOfGoods, s.product.cost * s.quantity)), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId)")
    Double sumCogsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    // ── Analytics: daily series ───────────────────────────────────────────────

    /**
     * Revenue and quantity grouped by sale date — used to build daily chart series.
     * Returns List of Object[]{LocalDate saleDate, Double revenue, Long quantity}.
     */
    @Query("SELECT s.saleDate, COALESCE(SUM(s.total), 0), COALESCE(SUM(s.quantity), 0) " +
           "FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY s.saleDate ORDER BY s.saleDate")
    List<Object[]> sumBySaleDate(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    /**
     * COGS grouped by sale date — used to compute daily gross profit.
     * Returns List of Object[]{LocalDate saleDate, Double cogs}.
     */
    @Query("SELECT s.saleDate, COALESCE(SUM(COALESCE(s.costOfGoods, s.product.cost * s.quantity)), 0) " +
           "FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY s.saleDate ORDER BY s.saleDate")
    List<Object[]> sumCogsBySaleDate(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    // ── Analytics: monthly series — EXTRACT is PostgreSQL / Neon safe ─────────

    /**
     * Revenue and quantity grouped by year and month.
     * Uses EXTRACT(YEAR/MONTH FROM ...) — works on PostgreSQL and Neon.
     * Returns List of Object[]{Double year, Double month, Double revenue, Long quantity}.
     */
    @Query("SELECT EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate), " +
           "COALESCE(SUM(s.total), 0), COALESCE(SUM(s.quantity), 0) " +
           "FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate) " +
           "ORDER BY EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate)")
    List<Object[]> sumByYearMonth(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    /**
     * COGS grouped by year and month — used to compute monthly gross profit.
     * Returns List of Object[]{Double year, Double month, Double cogs}.
     */
    @Query("SELECT EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate), " +
           "COALESCE(SUM(COALESCE(s.costOfGoods, s.product.cost * s.quantity)), 0) " +
           "FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate) " +
           "ORDER BY EXTRACT(YEAR FROM s.saleDate), EXTRACT(MONTH FROM s.saleDate)")
    List<Object[]> sumCogsByYearMonth(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);

    // ── Analytics: product revenue ranking ───────────────────────────────────

    /**
     * Product name, total revenue, and total quantity sold — ordered by revenue DESC.
     * Used to build top-products and bottom-products charts.
     * Returns List of Object[]{String productName, Double revenue, Long quantity}.
     */
    @Query("SELECT s.product.name, COALESCE(SUM(s.total), 0), COALESCE(SUM(s.quantity), 0) " +
           "FROM Sale s WHERE s.saleDate BETWEEN :from AND :to " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY s.product.name ORDER BY SUM(s.total) DESC")
    List<Object[]> findProductRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("branchId") Long branchId);
}