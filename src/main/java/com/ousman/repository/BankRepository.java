package com.ousman.repository;

import com.ousman.model.Bank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    Optional<Bank> findByNameIgnoreCase(String name);

    /** Only banks available for selection on the Sales form. */
    @Query("SELECT b FROM Bank b WHERE b.active = true ORDER BY b.name ASC")
    List<Bank> findAllActive();

    /** Search matches name, branch, or account number — case-insensitive substring. */
    @Query("SELECT b FROM Bank b WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.branch) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.name ASC")
    List<Bank> search(@Param("search") String search);

    @Query("SELECT b FROM Bank b WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.branch) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Bank> searchPage(@Param("search") String search, Pageable pageable);
}
