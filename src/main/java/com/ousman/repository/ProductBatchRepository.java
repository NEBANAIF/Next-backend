package com.ousman.repository;

import com.ousman.model.ProductBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    /**
     * Open batches for a product in a specific branch, oldest received
     * first — the FIFO consumption order used by automatic sale allocation
     * and shown first in the manual batch picker.
     */
    @Query("SELECT b FROM ProductBatch b WHERE b.product.id = :productId AND b.branch.id = :branchId " +
           "AND b.quantityRemaining > 0 ORDER BY b.receivedDate ASC, b.id ASC")
    List<ProductBatch> findAvailableFifo(@Param("productId") Long productId, @Param("branchId") Long branchId);

    /** Every batch (including depleted ones) for a product in a branch — audit/history view. */
    @Query("SELECT b FROM ProductBatch b WHERE b.product.id = :productId AND b.branch.id = :branchId " +
           "ORDER BY b.receivedDate DESC, b.id DESC")
    List<ProductBatch> findAllForProductAndBranch(@Param("productId") Long productId, @Param("branchId") Long branchId);

    /** Total remaining units of a product across every branch — should track Product.stock. */
    @Query("SELECT COALESCE(SUM(b.quantityRemaining), 0) FROM ProductBatch b WHERE b.product.id = :productId")
    Integer sumRemainingByProduct(@Param("productId") Long productId);

    /** Total remaining units of a product in one branch — per-location stock. */
    @Query("SELECT COALESCE(SUM(b.quantityRemaining), 0) FROM ProductBatch b WHERE b.product.id = :productId AND b.branch.id = :branchId")
    Integer sumRemainingByProductAndBranch(@Param("productId") Long productId, @Param("branchId") Long branchId);

    @Query("SELECT b FROM ProductBatch b WHERE " +
           "(:productId IS NULL OR b.product.id = :productId) AND " +
           "(:branchId IS NULL OR b.branch.id = :branchId) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(b.product.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.batchNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.supplier) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.receivedDate DESC, b.id DESC")
    Page<ProductBatch> searchPage(@Param("productId") Long productId, @Param("branchId") Long branchId,
                                   @Param("search") String search, Pageable pageable);

    // ── Branch-scoped dashboard aggregates ─────────────────────────────────
    // The product catalog is shared/global (Product.stock is the company-wide
    // total), but when the dashboard is filtered to one branch or one
    // location type, "stock on hand" has to mean stock in THOSE branches —
    // computed from open batches, not the global Product.stock field.

    /** Per-product remaining stock, summed across just these branches. */
    @Query("SELECT b.product.id, COALESCE(SUM(b.quantityRemaining), 0) FROM ProductBatch b " +
           "WHERE b.branch.id IN :branchIds GROUP BY b.product.id")
    List<Object[]> sumRemainingGroupedByProduct(@Param("branchIds") List<Long> branchIds);

    /** Total value of stock sitting in these branches, at each unit's own batch cost. */
    @Query("SELECT COALESCE(SUM(b.quantityRemaining * b.costPerUnit), 0) FROM ProductBatch b " +
           "WHERE b.branch.id IN :branchIds")
    Double sumInventoryValueByBranches(@Param("branchIds") List<Long> branchIds);
}
