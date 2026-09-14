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

    List<Customer> findByBranchIdOrderByNameAsc(Long branchId);

    @Query("SELECT c FROM Customer c WHERE c.branch.id IN :branchIds ORDER BY c.name ASC")
    List<Customer> findByBranchIdInOrderByNameAsc(@Param("branchIds") List<Long> branchIds);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    Page<Customer> search(@Param("search") String search, Pageable pageable);

    /** Same as search() above, restricted to a specific set of branches — used for scoped-role users. */
    @Query("SELECT c FROM Customer c WHERE " +
           "c.branch.id IN :branchIds AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    Page<Customer> searchByBranches(@Param("branchIds") List<Long> branchIds, @Param("search") String search, Pageable pageable);
}
