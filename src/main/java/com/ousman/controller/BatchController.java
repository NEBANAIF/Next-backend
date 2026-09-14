package com.ousman.controller;

import com.ousman.model.ProductBatch;
import com.ousman.service.AccessControlService;
import com.ousman.service.BatchService;
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
 *  Batch Controller — Role-Based Access
 *
 *  ADMIN:  full access, including editing a batch's cost/notes and deleting
 *          an untouched batch
 *  WORKER: can view batches and receive new stock into a batch (same
 *          access WORKER already has for plain stock additions on
 *          Products), but cannot edit or delete a batch afterwards
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/batches")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private AccessControlService accessControl;

    // Open batches only (remaining > 0), FIFO order — powers the manual
    // batch-picker dropdown on the Sales form.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<List<ProductBatch>> getAvailable(
            @RequestParam Long productId, @RequestParam Long branchId) {
        return ResponseEntity.ok(batchService.getAvailable(productId, branchId));
    }

    // Every batch (including depleted) for a product+branch — history view.
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<List<ProductBatch>> getAllForProductAndBranch(
            @RequestParam Long productId, @RequestParam Long branchId) {
        return ResponseEntity.ok(batchService.getAllForProductAndBranch(productId, branchId));
    }

    @GetMapping("/branch-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<?> getBranchStock(@RequestParam Long productId, @RequestParam Long branchId) {
        return ResponseEntity.ok(Map.of("stock", batchService.getBranchStock(productId, branchId)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<Page<ProductBatch>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedDate"));
        return ResponseEntity.ok(batchService.getPage(productId, branchId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(batchService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Receive stock — both roles, same as a plain stock addition ──────

    public static class ReceiveRequest {
        public Long productId;
        public Long branchId;
        public Integer quantity;
        public Double costPerUnit;
        public String batchNumber;
        public LocalDate receivedDate;
        public String supplier;
        public String notes;
        public String recordedBy;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')")
    public ResponseEntity<?> receive(@RequestBody ReceiveRequest req) {
        try {
            accessControl.requireBranchAccess(req.branchId);
            ProductBatch batch = batchService.receive(
                req.productId, req.branchId, req.quantity, req.costPerUnit,
                req.batchNumber, req.receivedDate,
                req.supplier, req.notes, req.recordedBy
            );
            return ResponseEntity.ok(batch);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN only: correct a batch's details, or delete an untouched one ─

    public static class UpdateRequest {
        public Double costPerUnit;
        public String batchNumber;
        public String supplier;
        public String notes;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        try {
            ProductBatch batch = batchService.update(
                id, req.costPerUnit, req.batchNumber, req.supplier, req.notes);
            return ResponseEntity.ok(batch);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            batchService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Batch deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
