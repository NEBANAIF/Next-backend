package com.ousman.repository;

import com.ousman.model.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    List<StockHistory> findByProductId(Long productId);

    List<StockHistory> findByType(String type);

    @Query("SELECT s FROM StockHistory s ORDER BY s.date DESC, s.time DESC")
    List<StockHistory> findAllOrderedByDate();

    @Query("SELECT s FROM StockHistory s WHERE (:branchId IS NULL OR s.branch.id = :branchId) ORDER BY s.date DESC, s.time DESC")
    List<StockHistory> findAllOrderedByDate(@org.springframework.data.repository.query.Param("branchId") Long branchId);
}