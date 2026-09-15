package com.ousman.controller;

import com.ousman.service.ReturnService;
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
@RequestMapping("/api/returns")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private com.ousman.service.AccessControlService accessControl;

    private static final String OPERATIONAL_ROLES =
        "hasAnyRole('ADMIN', 'WORKER', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    @GetMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> search(@RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(returnService.search(accessControl.resolveBranchFilter(branchId)));
    }

    @GetMapping("/page")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<Page<com.ousman.model.Return>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long branchId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(returnService.getPage(accessControl.resolveBranchFilter(branchId), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(returnService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/for-sale/{saleId}")
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> getForSale(@PathVariable Long saleId) {
        return ResponseEntity.ok(returnService.getForSale(saleId));
    }

    public static class CreateRequest {
        public Long saleId;
        public Integer quantity;
        public String reason;
        public Boolean restock;
        public Double refundAmount;
        public String processedBy;
    }

    @PostMapping
    @PreAuthorize(OPERATIONAL_ROLES)
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        try {
            return ResponseEntity.ok(returnService.create(
                req.saleId, req.quantity, req.reason, req.restock, req.refundAmount, req.processedBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
