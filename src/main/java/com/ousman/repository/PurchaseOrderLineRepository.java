package com.ousman.repository;

import com.ousman.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    @Query("SELECT l FROM PurchaseOrderLine l WHERE l.purchaseOrder.id = :poId ORDER BY l.id ASC")
    List<PurchaseOrderLine> findByPurchaseOrderId(@Param("poId") Long poId);

    boolean existsByProductId(Long productId);
}
