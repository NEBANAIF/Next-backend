package com.ousman.controller;

import com.ousman.model.PurchaseOrder;
import com.ousman.service.PurchaseService;
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
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<Page<PurchaseOrder>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(purchaseService.search(status, pageable));
    }

    @GetMapping("/history")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<PurchaseOrder>> getHistory() {
        return ResponseEntity.ok(purchaseService.getHistory());
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(purchaseService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Create ───────────────────────────────────────────────────────────

    public static class CreateRequest {
        public Long supplierId;
        public Long branchId;
        public List<PurchaseService.LineItemRequest> items;
        public String notes;
        public String orderedBy;
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        try {
            return ResponseEntity.ok(purchaseService.create(
                req.supplierId, req.branchId, req.items, req.notes, req.orderedBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Receive ──────────────────────────────────────────────────────────

    public static class ReceiveItemRequest {
        public Long itemId;
        public Integer quantity;
        public String receivedBy;
    }

    @PostMapping("/{id}/receive-item")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> receiveItem(@PathVariable Long id, @RequestBody ReceiveItemRequest req) {
        try {
            return ResponseEntity.ok(purchaseService.receiveItem(id, req.itemId, req.quantity, req.receivedBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public static class ActorRequest {
        public String actor;
    }

    @PostMapping("/{id}/receive-all")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> receiveAll(@PathVariable Long id, @RequestBody(required = false) ActorRequest req) {
        try {
            String actor = req != null ? req.actor : null;
            return ResponseEntity.ok(purchaseService.receiveAll(id, actor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) ActorRequest req) {
        try {
            String actor = req != null ? req.actor : null;
            return ResponseEntity.ok(purchaseService.cancel(id, actor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
