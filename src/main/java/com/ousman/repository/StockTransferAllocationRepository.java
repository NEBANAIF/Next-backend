package com.ousman.repository;

import com.ousman.model.StockTransferAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferAllocationRepository extends JpaRepository<StockTransferAllocation, Long> {

    @Query("SELECT a FROM StockTransferAllocation a WHERE a.transfer.id = :transferId")
    List<StockTransferAllocation> findByTransferId(@Param("transferId") Long transferId);
}
