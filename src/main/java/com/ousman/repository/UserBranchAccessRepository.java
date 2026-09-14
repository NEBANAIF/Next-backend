package com.ousman.repository;

import com.ousman.model.UserBranchAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBranchAccessRepository extends JpaRepository<UserBranchAccess, Long> {

    @Query("SELECT a FROM UserBranchAccess a WHERE a.user.id = :userId ORDER BY a.branch.name ASC")
    List<UserBranchAccess> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM UserBranchAccess a WHERE a.branch.id = :branchId ORDER BY a.user.name ASC")
    List<UserBranchAccess> findByBranchId(@Param("branchId") Long branchId);

    Optional<UserBranchAccess> findByUserIdAndBranchId(Long userId, Long branchId);

    boolean existsByUserIdAndBranchId(Long userId, Long branchId);

    boolean existsByBranchId(Long branchId);

    void deleteByUserIdAndBranchId(Long userId, Long branchId);
}
