package com.ousman.service;

import com.ousman.model.Product;
import com.ousman.model.StockHistory;
import com.ousman.model.Branch;
import com.ousman.model.ProductBatch;
import com.ousman.repository.ProductRepository;
import com.ousman.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Product service — all read operations are cached under the "products" cache.
 * Any write operation (create / update / delete / adjustStock) evicts the cache
 * so the next read always fetches fresh data from Neon PostgreSQL.
 *
 * Cache strategy:
 *  @Cacheable("products")          → cache the result, return cached on next call
 *  @CacheEvict(allEntries=true)    → wipe entire "products" cache on any mutation
 *
 * Load balancing note:
 *  Each backend replica has its own in-process Caffeine cache. A write on replica A
 *  evicts replica A's cache. Replica B's cache expires naturally within the TTL
 *  configured in CacheConfig (10 minutes). This is the correct trade-off for an ERP
 *  with infrequent writes and high read traffic.
 */
@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    // ── Read: cached ─────────────────────────────────────

    /**
     * Returns all products. Result is cached under key "allProducts".
     * Cache is evicted whenever any product is created, updated, or deleted.
     */
    @Cacheable(value = "products", key = "'allProducts'")
    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    /**
     * Returns a single product by ID. Cached per product ID.
     * Cache is evicted on any mutation that touches this product.
     */
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Full-text search across name and category.
     * Cached per keyword so repeated identical searches are instant.
     */
    @Cacheable(value = "products", key = "'search:' + #keyword")
    @Transactional(readOnly = true)
    public List<Product> search(String keyword) {
        return productRepository.search(keyword);
    }

    /**
     * Returns products in LOW_STOCK status.
     * Cached separately — evicted on any product mutation.
     */
    @Cacheable(value = "products", key = "'lowStock'")
    @Transactional(readOnly = true)
    public List<Product> getLowStock() {
        return productRepository.findLowStock();
    }

    /**
     * Returns products with zero stock.
     * Cached separately — evicted on any product mutation.
     */
    @Cacheable(value = "products", key = "'outOfStock'")
    @Transactional(readOnly = true)
    public List<Product> getOutOfStock() {
        return productRepository.findOutOfStock();
    }

    // ── Write: cache-evicting ─────────────────────────────

    /**
     * Creates a new product.
     * Evicts the entire "products" cache so all lists are refreshed.
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product create(Product product) {
        if (product.getCost()     == null) product.setCost(0.0);
        if (product.getStock()    == null) product.setStock(0);
        if (product.getMinStock() == null) product.setMinStock(30);

        Product saved = productRepository.save(product);

        // Record initial stock in history if stock > 0
        if (saved.getStock() > 0) {
            recordHistory(saved, saved.getStock(), 0, saved.getStock(),
                    "STOCK_ADDITION", "Initial stock", "System", "INIT-" + saved.getId());
        }
        return saved;
    }

    /**
     * Updates an existing product's details (not stock — use adjustStock for that).
     * Evicts the entire "products" cache.
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product update(Long id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        existing.setName(updated.getName());
        existing.setPrice(updated.getPrice());
        existing.setCost(updated.getCost() != null ? updated.getCost() : 0.0);
        existing.setMinStock(updated.getMinStock() != null ? updated.getMinStock() : 30);
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        // Stock is managed separately via adjustStock — never changed here
        return productRepository.save(existing);
    }

    /**
     * Adjusts (adds or subtracts) stock for a product.
     * Evicts the entire "products" cache.
     *
     * @param id             product ID
     * @param quantityChange positive = add stock, negative = remove stock
     * @param reason         human-readable reason for the change
     * @param user           who made the change
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product adjustStock(Long id, int quantityChange, String reason, String user) {
        return adjustStock(id, quantityChange, reason, user, null, null);
    }

    /**
     * Same as above, but also tags the resulting StockHistory row with the
     * branch this movement happened at and (when known) the specific batch
     * it came from — used by BatchService, which always has both at hand.
     */
    @CacheEvict(value = "products", allEntries = true)
    public Product adjustStock(Long id, int quantityChange, String reason, String user,
                                Branch branch, ProductBatch batch) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        int prevStock = product.getStock();
        int newStock  = prevStock + quantityChange;

        if (newStock < 0) {
            throw new RuntimeException(
                "Insufficient stock. Available: " + prevStock +
                ", requested: " + Math.abs(quantityChange));
        }

        product.setStock(newStock);
        Product saved = productRepository.save(product);

        String type = quantityChange > 0 ? "STOCK_ADDITION" : "ADJUSTMENT";
        recordHistory(saved, quantityChange, prevStock, newStock, type, reason, user, null, branch, batch);
        return saved;
    }

    /**
     * Deletes a product by ID.
     * Evicts the entire "products" cache.
     */
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    // ── Internal: record stock history ───────────────────

    /**
     * Saves a StockHistory row for any stock-changing operation.
     * Called internally by create, adjustStock, and SaleService.
     * Does NOT evict the products cache — caller handles that.
     */
    public void recordHistory(Product product, int quantityChange,
                               int prevStock, int newStock,
                               String type, String reason,
                               String user, String reference) {
        recordHistory(product, quantityChange, prevStock, newStock, type, reason, user, reference, null, null);
    }

    /**
     * Full version — also tags the row with the branch this movement
     * happened at and, when known, the specific batch it came from.
     * Both are nullable: legacy flat-stock rows and a few manual-adjustment
     * paths genuinely have neither.
     */
    public void recordHistory(Product product, int quantityChange,
                               int prevStock, int newStock,
                               String type, String reason,
                               String user, String reference,
                               Branch branch, ProductBatch batch) {
        StockHistory history = new StockHistory();
        history.setProduct(product);
        history.setQuantityChange(quantityChange);
        history.setPreviousStock(prevStock);
        history.setNewStock(newStock);
        history.setType(type);
        history.setReason(reason);
        history.setUser(user);
        history.setReference(reference);
        history.setBranch(branch);
        history.setBatch(batch);
        // Explicitly set date and time so they are never null,
        // even if @PrePersist is bypassed (e.g. old records, bulk inserts)
        history.setDate(java.time.LocalDate.now());
        history.setTime(java.time.LocalTime.now());
        stockHistoryRepository.save(history);
    }
}