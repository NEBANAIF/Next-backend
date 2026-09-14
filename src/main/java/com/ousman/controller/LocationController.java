package com.ousman.controller;

import com.ousman.model.Location;
import com.ousman.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Location Controller — Role-Based Access
 *
 *  A Location is a Store or a Warehouse. Creating one auto-creates its
 *  "Main" branch (see LocationService.create) — additional branches are
 *  added via BranchController.
 *
 *  ADMIN:  full CRUD
 *  Everyone else: read-only (needed to populate location pickers, and to
 *  see which location a branch belongs to)
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    private static final String READ_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<List<Location>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(locationService.getAll(search));
    }

    @GetMapping("/active")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<List<Location>> getActive(@RequestParam(required = false) String type) {
        return ResponseEntity.ok((type == null || type.isBlank())
            ? locationService.getActive()
            : locationService.getActiveByType(type));
    }

    @GetMapping("/page")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Page<Location>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(locationService.getPage(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(locationService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN only: create / update / delete ────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Location location) {
        try {
            return ResponseEntity.ok(locationService.create(location));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Location location) {
        try {
            return ResponseEntity.ok(locationService.update(id, location));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            locationService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Location deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
