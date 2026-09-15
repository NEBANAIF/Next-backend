package com.ousman.repository;

import com.ousman.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Product repository — includes all methods called by AnalyticsService
 * and ProductService.
 *
 * Analytics methods provided:
 *  - countByStatus(String)        → count IN_STOCK / LOW_STOCK / OUT_OF_STOCK
 *  - sumInventoryValue()          → total value of all stock
 *  - countHighStockProducts()     → products well above minimum threshold
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    /** All products with a specific status string */
    List<Product> findByStatus(String status);

    /** All products in a specific category */
    List<Product> findByCategoryId(Long categoryId);

    /**
     * Full-text search across name and category name (case-insensitive).
     * Uses JPQL LOWER + CONCAT — works on both PostgreSQL and H2.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> search(@Param("keyword") String keyword);

    /** Products with stock ≤ minStock but greater than zero (low stock warning) */
    @Query("SELECT p FROM Product p WHERE p.stock <= p.minStock AND p.stock > 0")
    List<Product> findLowStock();

    /** Products completely out of stock */
    @Query("SELECT p FROM Product p WHERE p.stock = 0")
    List<Product> findOutOfStock();

    // ── Analytics counts ──────────────────────────────────────────────────────

    /**
     * Count products by their status string.
     * Called with "IN_STOCK", "LOW_STOCK", or "OUT_OF_STOCK".
     * Returns primitive long — safe to assign directly without unboxing.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Total inventory value across all products: SUM(cost × stock).
     * Returns Double — wrapped with COALESCE so never null when no products exist.
     * Called by AnalyticsService for the inventory value KPI card.
     */
    @Query("SELECT COALESCE(SUM(p.cost * p.stock), 0) FROM Product p")
    Double sumInventoryValue();

    /**
     * Count of products with stock well above their minimum threshold (stock > minStock × 2).
     * Used to identify overstocked items in the analytics dashboard.
     * Returns primitive long — safe to assign directly without unboxing.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock > p.minStock * 2")
    long countHighStockProducts();
}