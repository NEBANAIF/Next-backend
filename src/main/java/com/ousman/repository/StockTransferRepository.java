package com.ousman.repository;

import com.ousman.model.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @Query("SELECT t FROM StockTransfer t WHERE t.status = 'PENDING' ORDER BY t.requestedAt DESC")
    List<StockTransfer> findAllPending();

    @Query("SELECT t FROM StockTransfer t WHERE " +
           "(:status IS NULL OR :status = '' OR t.status = :status) AND " +
           "(:branchId IS NULL OR t.fromBranch.id = :branchId OR t.toBranch.id = :branchId) " +
           "ORDER BY t.requestedAt DESC")
    List<StockTransfer> search(@Param("status") String status, @Param("branchId") Long branchId);

    @Query("SELECT t FROM StockTransfer t WHERE " +
           "(:status IS NULL OR :status = '' OR t.status = :status) AND " +
           "(:branchId IS NULL OR t.fromBranch.id = :branchId OR t.toBranch.id = :branchId) " +
           "ORDER BY t.requestedAt DESC")
    Page<StockTransfer> searchPage(@Param("status") String status, @Param("branchId") Long branchId, Pageable pageable);

    boolean existsByFromBranchIdOrToBranchId(Long fromBranchId, Long toBranchId);
}
