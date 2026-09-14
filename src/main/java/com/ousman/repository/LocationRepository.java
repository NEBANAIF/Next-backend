package com.ousman.repository;

import com.ousman.model.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByNameIgnoreCase(String name);

    @Query("SELECT l FROM Location l WHERE l.active = true ORDER BY l.name ASC")
    List<Location> findAllActive();

    @Query("SELECT l FROM Location l WHERE l.active = true AND l.type = :type ORDER BY l.name ASC")
    List<Location> findAllActiveByType(@Param("type") String type);

    @Query("SELECT l FROM Location l WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.managerName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY l.name ASC")
    List<Location> search(@Param("search") String search);

    @Query("SELECT l FROM Location l WHERE " +
           "(:search IS NULL OR :search = '' " +
           "  OR LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(l.managerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Location> searchPage(@Param("search") String search, Pageable pageable);
}
