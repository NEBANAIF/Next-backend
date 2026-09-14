package com.ousman.controller;

import com.ousman.model.Supplier;
import com.ousman.service.SupplierService;
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

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<Supplier>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @GetMapping("/active")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<Supplier>> getActive() {
        return ResponseEntity.ok(supplierService.getActive());
    }

    @GetMapping("/page")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<Page<Supplier>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(supplierService.search(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(supplierService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> create(@RequestBody Supplier supplier) {
        try {
            return ResponseEntity.ok(supplierService.create(supplier));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            return ResponseEntity.ok(supplierService.update(id, supplier));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            supplierService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Supplier deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
