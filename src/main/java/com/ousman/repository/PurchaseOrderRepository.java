package com.ousman.repository;

import com.ousman.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    boolean existsBySupplierId(Long supplierId);

    boolean existsByBranchId(Long branchId);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:branchId IS NULL OR po.branch.id = :branchId) AND " +
           "(:status IS NULL OR :status = '' OR po.status = :status) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(po.supplier.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY po.createdAt DESC")
    List<PurchaseOrder> search(@Param("branchId") Long branchId, @Param("status") String status, @Param("search") String search);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:branchId IS NULL OR po.branch.id = :branchId) AND " +
           "(:status IS NULL OR :status = '' OR po.status = :status) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(po.supplier.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PurchaseOrder> searchPage(@Param("branchId") Long branchId, @Param("status") String status,
                                    @Param("search") String search, Pageable pageable);
}
