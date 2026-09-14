package com.ousman.repository;

import com.ousman.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByActiveTrueOrderByNameAsc();

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY s.name ASC")
    Page<Supplier> search(@Param("search") String search, Pageable pageable);
}
