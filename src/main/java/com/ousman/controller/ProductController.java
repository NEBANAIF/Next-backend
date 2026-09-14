package com.ousman.controller;

import com.ousman.model.Product;
import com.ousman.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // GET /api/products
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // GET /api/products/search?keyword=camera
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.search(keyword));
    }

    // GET /api/products/low-stock
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStock() {
        return ResponseEntity.ok(productService.getLowStock());
    }

    // GET /api/products/out-of-stock
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<Product>> getOutOfStock() {
        return ResponseEntity.ok(productService.getOutOfStock());
    }

    // POST /api/products
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.create(product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/products/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.update(id, product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/products/{id}/adjust-stock?quantity=10&reason=Restock&user=Admin
    @PostMapping("/{id}/adjust-stock")
    public ResponseEntity<?> adjustStock(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "Stock adjustment") String reason,
            @RequestParam(defaultValue = "Admin") String user) {
        try {
            return ResponseEntity.ok(productService.adjustStock(id, quantity, reason, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Backward compatible: POST /api/products/{id}/add-stock
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<?> addStock(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "Stock addition") String reason,
            @RequestParam(defaultValue = "Admin") String user) {
        try {
            return ResponseEntity.ok(productService.adjustStock(id, quantity, reason, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}