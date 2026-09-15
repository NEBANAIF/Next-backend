package com.ousman.repository;

import com.ousman.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.branch.id = :branchId AND c.active = true ORDER BY c.name ASC")
    List<Customer> findAllActiveForBranch(@Param("branchId") Long branchId);

    boolean existsByBranchId(Long branchId);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:branchId IS NULL OR c.branch.id = :branchId) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    List<Customer> search(@Param("branchId") Long branchId, @Param("search") String search);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:branchId IS NULL OR c.branch.id = :branchId) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchPage(@Param("branchId") Long branchId, @Param("search") String search, Pageable pageable);
}
