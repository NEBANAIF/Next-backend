package com.ousman.controller;

import com.ousman.model.Customer;
import com.ousman.service.CustomerService;
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
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // Every operational role (ADMIN, legacy WORKER, and the three
    // location-scoped roles) can manage customers — always narrowed to
    // their own branch(es) except for ADMIN.
    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long branchId) {
        try {
            return ResponseEntity.ok(customerService.getAll(branchId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/page")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<Page<Customer>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(customerService.search(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(customerService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> create(@RequestBody Customer customer) {
        try {
            return ResponseEntity.ok(customerService.create(customer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Customer customer) {
        try {
            return ResponseEntity.ok(customerService.update(id, customer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            customerService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Customer deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
