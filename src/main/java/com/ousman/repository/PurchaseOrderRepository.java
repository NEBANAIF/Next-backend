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

    @Query("SELECT p FROM PurchaseOrder p WHERE " +
           "(:status IS NULL OR :status = '' OR p.status = :status) " +
           "ORDER BY p.createdAt DESC")
    Page<PurchaseOrder> search(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM PurchaseOrder p WHERE " +
           "p.branch.id IN :branchIds AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status) " +
           "ORDER BY p.createdAt DESC")
    Page<PurchaseOrder> searchByBranches(@Param("branchIds") List<Long> branchIds,
                                          @Param("status") String status, Pageable pageable);

    // "Purchase History" — every order that has ever had anything received
    // against it (fully or partially), newest first. Distinct from the
    // general search() above, which includes DRAFT/ORDERED/CANCELLED too.
    @Query("SELECT p FROM PurchaseOrder p WHERE p.status IN ('PARTIALLY_RECEIVED', 'RECEIVED') ORDER BY p.updatedAt DESC")
    List<PurchaseOrder> findHistory();

    @Query("SELECT p FROM PurchaseOrder p WHERE p.branch.id IN :branchIds AND " +
           "p.status IN ('PARTIALLY_RECEIVED', 'RECEIVED') ORDER BY p.updatedAt DESC")
    List<PurchaseOrder> findHistoryByBranches(@Param("branchIds") List<Long> branchIds);
}
