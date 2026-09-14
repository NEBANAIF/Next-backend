package com.ousman.controller;

import com.ousman.model.Bank;
import com.ousman.service.BankService;
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
 *  Bank Controller — Role-Based Access
 *
 *  ADMIN:  full CRUD (manage the bank list from the Payments page)
 *  WORKER: read-only — needs the list to pick a bank on the Sales form,
 *          but cannot create/edit/delete banks
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/banks")
public class BankController {

    @Autowired
    private BankService bankService;

    // GET /api/banks?search= — full list, both roles (management table)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<Bank>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(bankService.getAll(search));
    }

    // GET /api/banks/active — active-only, lightweight list for the Sales-form picker
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<Bank>> getActive() {
        return ResponseEntity.ok(bankService.getActive());
    }

    // GET /api/banks/page — paginated + searchable, powers the Banks management table
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<Page<Bank>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(bankService.getPage(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(bankService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── ADMIN only: create / update / delete ────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Bank bank) {
        try {
            return ResponseEntity.ok(bankService.create(bank));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Bank bank) {
        try {
            return ResponseEntity.ok(bankService.update(id, bank));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            bankService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Bank deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
