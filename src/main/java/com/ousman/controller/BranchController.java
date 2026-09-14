package com.ousman.controller;

import com.ousman.model.Branch;
import com.ousman.service.BranchService;
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
 *  Branch Controller — Role-Based Access
 *
 *  A branch always belongs to exactly one Location (see LocationController
 *  for creating locations, which auto-creates each one's "Main" branch).
 *  This controller is for adding *additional* branches under an existing
 *  location, and for everyday CRUD/listing.
 *
 *  ADMIN:              full CRUD across every branch
 *  WORKER (legacy):     read-only, unscoped — sees every branch, same as
 *                       before branches existed
 *  WAREHOUSE_MANAGER,
 *  STORE_MANAGER,
 *  STAFF:               read-only, and only for branches assigned to them
 *                       (BranchService.getActive() already filters this;
 *                       write access is admin-only regardless of role)
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    private static final String READ_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<List<Branch>> getAll(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(branchService.getAll(locationId, search));
    }

    // Active branches — filtered to the current user's assigned branches
    // for the location-scoped roles; unfiltered for ADMIN/WORKER. This is
    // what feeds every branch picker (sales, receiving, transfers).
    @GetMapping("/active")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<List<Branch>> getActive() {
        return ResponseEntity.ok(branchService.getActive());
    }

    @GetMapping("/by-location/{locationId}")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<List<Branch>> getByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(branchService.getByLocation(locationId));
    }

    @GetMapping("/page")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<Page<Branch>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(branchService.getPage(locationId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(branchService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN only: create / update / delete ────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Branch branch) {
        try {
            return ResponseEntity.ok(branchService.create(branch));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Branch branch) {
        try {
            return ResponseEntity.ok(branchService.update(id, branch));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            branchService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Branch deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
