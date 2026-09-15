package com.ousman.repository;

import com.ousman.model.Return;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {

    @Query("SELECT r FROM Return r WHERE r.sale.id = :saleId ORDER BY r.createdAt ASC")
    List<Return> findBySaleId(@Param("saleId") Long saleId);

    @Query("SELECT r FROM Return r WHERE " +
           "(:branchId IS NULL OR r.branch.id = :branchId) " +
           "ORDER BY r.createdAt DESC")
    List<Return> search(@Param("branchId") Long branchId);

    @Query("SELECT r FROM Return r WHERE " +
           "(:branchId IS NULL OR r.branch.id = :branchId)")
    Page<Return> searchPage(@Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Return r WHERE r.sale.id = :saleId")
    Integer sumReturnedQuantityForSale(@Param("saleId") Long saleId);
}
