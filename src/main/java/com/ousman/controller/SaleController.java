package com.ousman.controller;

import com.ousman.model.Sale;
import com.ousman.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Sale Controller — Role-Based Access
 *
 *  ADMIN:  GET all, GET by id, POST (record), PUT payment (loans), DELETE
 *  WORKER: GET all, GET /today, POST (record sale), PUT payment (loans)
 *          GET by id → 403 (admin only) · DELETE → 403 (blocked)
 *
 *  The /today endpoint is used by the frontend when the logged-in user is WORKER.
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired
    private SaleService saleService;

    // ── GET /api/sales — ADMIN + WORKER (view full sales history) ─────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<List<Sale>> getAll() {
        return ResponseEntity.ok(saleService.getAll());
    }

    // ── GET /api/sales/page — ADMIN + WORKER (paginated, searchable listing) ──
    // This is the endpoint the Sales page table uses. Unlike getAll() above,
    // it only loads one page of rows from the database (LIMIT/OFFSET under
    // the hood), so it stays fast and the response stays small no matter
    // how many sales pile up over time — the old approach of fetching
    // everything and paging through it in the browser doesn't scale past
    // a few thousand rows.
    //
    //   page   — zero-based page index (default 0)
    //   size   — rows per page (default 20)
    //   search — matches customer name or product name, case-insensitive
    //   date   — filter to one sale date, e.g. ?date=2026-07-22
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<Page<Sale>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDate", "saleTime"));
        return ResponseEntity.ok(saleService.search(search, date, pageable));
    }

    // ── GET /api/sales/today — WORKER + ADMIN (only today's sales) ────────
    // Workers call this endpoint; admins can also use it for a quick today view
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<List<Sale>> getToday() {
        // Query only today's rows at the database level (indexed on sale_date)
        // instead of loading the entire sales table into memory and filtering
        // in Java — this used to load and deserialize every historical sale
        // just to throw most of it away, which gets slower every day as the
        // table grows.
        return ResponseEntity.ok(saleService.getByDate(LocalDate.now()));
    }

    // ── GET /api/sales/{id} — ADMIN only ──────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Sale> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    // ── POST /api/sales — ADMIN + WORKER (record a new sale) ──────────────
    // Both roles can record sales
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<?> recordSale(
            @RequestBody Sale sale,
            @AuthenticationPrincipal String email) {
        try {
            // Auto-tag who recorded the sale based on the JWT principal (email)
            if (sale.getRecordedBy() == null || sale.getRecordedBy().isBlank()) {
                sale.setRecordedBy(email);
            }
            return ResponseEntity.ok(saleService.recordSale(sale));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/sales/{id}/payment — ADMIN + WORKER (record loan payment) ─
    // Body may optionally include paymentMethod ("CASH"/"BANK"), bankId,
    // transactionRef, and notes — when present, the newly-collected portion
    // of the repayment is logged as a Payment record (see PaymentService).
    // Omitting them keeps the old behavior (payment-fields-only update, no
    // linked Payment row), so existing callers keep working unchanged.
    @PutMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String email) {
        try {
            Double newPaidAmount = body.get("paidAmount") instanceof Number n
                    ? n.doubleValue()
                    : Double.parseDouble(body.get("paidAmount").toString());

            String paymentMethod  = (String) body.get("paymentMethod");
            String transactionRef = (String) body.get("transactionRef");
            String notes          = (String) body.get("notes");
            Long bankId = body.get("bankId") instanceof Number n2
                    ? n2.longValue()
                    : (body.get("bankId") != null ? Long.parseLong(body.get("bankId").toString()) : null);

            return ResponseEntity.ok(saleService.updateSalePayment(
                id, newPaidAmount, paymentMethod, bankId, transactionRef, notes, email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/sales/{id} — ADMIN only ───────────────────────────────
    // SecurityConfig already blocks DELETE for WORKER with 403
    // This @PreAuthorize is an extra layer of protection
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSale(@PathVariable Long id) {
        try {
            saleService.deleteSale(id);
            return ResponseEntity.ok(Map.of("message", "Sale deleted and stock restored"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
