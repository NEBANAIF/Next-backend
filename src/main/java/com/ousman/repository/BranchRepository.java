package com.ousman.repository;

import com.ousman.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    @Query("SELECT b FROM Branch b WHERE b.location.id = :locationId ORDER BY b.isDefault DESC, b.name ASC")
    List<Branch> findByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT b FROM Branch b WHERE b.location.id = :locationId AND b.isDefault = true")
    Optional<Branch> findDefaultForLocation(@Param("locationId") Long locationId);

    @Query("SELECT b FROM Branch b WHERE b.active = true ORDER BY b.name ASC")
    List<Branch> findAllActive();

    @Query("SELECT b FROM Branch b WHERE b.active = true AND b.location.id = :locationId ORDER BY b.isDefault DESC, b.name ASC")
    List<Branch> findAllActiveByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT b FROM Branch b WHERE b.active = true AND b.location.type = :locationType ORDER BY b.name ASC")
    List<Branch> findAllActiveByLocationType(@Param("locationType") String locationType);

    @Query("SELECT b FROM Branch b WHERE " +
           "(:locationId IS NULL OR b.location.id = :locationId) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.managerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.location.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.location.name ASC, b.isDefault DESC, b.name ASC")
    List<Branch> search(@Param("locationId") Long locationId, @Param("search") String search);

    @Query("SELECT b FROM Branch b WHERE " +
           "(:locationId IS NULL OR b.location.id = :locationId) AND " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.managerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(b.location.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Branch> searchPage(@Param("locationId") Long locationId, @Param("search") String search, Pageable pageable);

    boolean existsByLocationId(Long locationId);
}
