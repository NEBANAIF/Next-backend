package com.ousman.repository;

import com.ousman.model.SaleBatchAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleBatchAllocationRepository extends JpaRepository<SaleBatchAllocation, Long> {

    @Query("SELECT a FROM SaleBatchAllocation a WHERE a.sale.id = :saleId")
    List<SaleBatchAllocation> findBySaleId(@Param("saleId") Long saleId);

    @Query("SELECT a FROM SaleBatchAllocation a WHERE a.batch.id = :batchId")
    List<SaleBatchAllocation> findByBatchId(@Param("batchId") Long batchId);

    boolean existsByBatchId(Long batchId);
}
