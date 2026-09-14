package com.ousman.controller;

import com.ousman.model.StockHistory;
import com.ousman.service.StockHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-history")
public class StockHistoryController {

    @Autowired
    private StockHistoryService stockHistoryService;

    // Matches the URL-level rule in SecurityConfig: ADMIN plus the three
    // location-scoped roles. The legacy WORKER role keeps its original
    // restriction — no stock-history access.
    private static final String STOCK_HISTORY_ROLES =
        "hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'STORE_MANAGER', 'STAFF')";

    // GET /api/stock-history?branchId=  — branchId narrows to one branch;
    // scoped users are further filtered to branches they can see regardless.
    @GetMapping
    @PreAuthorize(STOCK_HISTORY_ROLES)
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long branchId) {
        try {
            return ResponseEntity.ok(stockHistoryService.getAllVisible(branchId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/stock-history/product/{productId}
    @GetMapping("/product/{productId}")
    @PreAuthorize(STOCK_HISTORY_ROLES)
    public ResponseEntity<List<StockHistory>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockHistoryService.getByProduct(productId));
    }

    // DELETE /api/stock-history/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            stockHistoryService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Record deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
