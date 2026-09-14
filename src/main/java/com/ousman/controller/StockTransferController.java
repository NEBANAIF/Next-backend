package com.ousman.controller;

import com.ousman.model.StockTransfer;
import com.ousman.service.StockTransferService;
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
 *  Stock Transfer Controller — Request → Approve → Complete
 *
 *  Any authenticated operational role can request a transfer between
 *  branches they can see. Approving/rejecting requires access to the
 *  SOURCE branch (releasing stock); completing requires access to the
 *  DESTINATION branch (confirming receipt). ADMIN can do all of the above
 *  regardless of branch. See StockTransferService / AccessControlService
 *  for the exact rules.
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/transfers")
public class StockTransferController {

    @Autowired
    private StockTransferService transferService;

    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping("/pending")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<StockTransfer>> getPending() {
        return ResponseEntity.ok(transferService.getPending());
    }

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<List<StockTransfer>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(transferService.search(status, branchId));
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<StockTransfer>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        return ResponseEntity.ok(transferService.getPage(status, branchId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(transferService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Request ──────────────────────────────────────────────────────────

    public static class RequestBody {
        public Long productId;
        public Long fromBranchId;
        public Long toBranchId;
        public Integer quantity;
        public Long batchId;
        public String notes;
        public String requestedBy;
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> request(@RequestBody RequestBody req) {
        try {
            StockTransfer transfer = transferService.request(
                req.productId, req.fromBranchId, req.toBranchId, req.quantity,
                req.batchId, req.notes, req.requestedBy);
            return ResponseEntity.ok(transfer);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Approve / Reject / Complete ──────────────────────────────────────

    public static class ActorBody {
        public String actor;
        public String reason; // only used by reject
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) ActorBody body) {
        try {
            String actor = body != null ? body.actor : null;
            return ResponseEntity.ok(transferService.approve(id, actor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) ActorBody body) {
        try {
            String actor  = body != null ? body.actor : null;
            String reason = body != null ? body.reason : null;
            return ResponseEntity.ok(transferService.reject(id, actor, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) ActorBody body) {
        try {
            String actor  = body != null ? body.actor : null;
            String reason = body != null ? body.reason : null;
            return ResponseEntity.ok(transferService.cancel(id, actor, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/in-transit")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> markInTransit(@PathVariable Long id, @RequestBody(required = false) ActorBody body) {
        try {
            String actor = body != null ? body.actor : null;
            return ResponseEntity.ok(transferService.markInTransit(id, actor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> complete(@PathVariable Long id, @RequestBody(required = false) ActorBody body) {
        try {
            String actor = body != null ? body.actor : null;
            return ResponseEntity.ok(transferService.complete(id, actor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
