package com.ousman.controller;

import com.ousman.model.ProductBatch;
import com.ousman.model.PurchaseOrder;
import com.ousman.model.PurchaseOrderLine;
import com.ousman.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Purchase Order Controller — Draft → Order → Receive (→ batches)
 *
 *  Every operational role can create/manage POs for branches they have
 *  access to (AccessControlService enforces this inside the service).
 *  ADMIN can act on any branch's orders regardless.
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private com.ousman.service.AccessControlService accessControl;

    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<PurchaseOrder>> search(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(poService.search(accessControl.resolveBranchFilter(branchId), status, search));
    }

    @GetMapping("/page")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<Page<PurchaseOrder>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(poService.getPage(accessControl.resolveBranchFilter(branchId), status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(poService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getLines(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(poService.getLines(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Create ───────────────────────────────────────────────────────────

    public static class CreateRequest {
        public Long branchId;
        public Long supplierId;
        public LocalDate expectedDate;
        public String notes;
        public String createdBy;
        public List<PurchaseOrderService.LineInput> lines;
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        try {
            PurchaseOrder po = poService.create(req.branchId, req.supplierId, req.expectedDate,
                    req.notes, req.createdBy, req.lines);
            return ResponseEntity.ok(po);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Status transitions ──────────────────────────────────────────────

    @PostMapping("/{id}/mark-ordered")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> markOrdered(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(poService.markOrdered(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(poService.cancel(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Receive against a line ───────────────────────────────────────────

    public static class ReceiveLineRequest {
        public Integer quantity;
        public String batchNumber;
        public LocalDate receivedDate;
        public String recordedBy;
    }

    @PostMapping("/{id}/lines/{lineId}/receive")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> receiveLine(@PathVariable Long id, @PathVariable Long lineId,
                                          @RequestBody ReceiveLineRequest req) {
        try {
            ProductBatch batch = poService.receiveLine(id, lineId, req.quantity, req.batchNumber,
                    req.receivedDate, req.recordedBy);
            return ResponseEntity.ok(batch);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
