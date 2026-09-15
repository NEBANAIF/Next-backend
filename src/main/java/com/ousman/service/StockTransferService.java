package com.ousman.service;

import com.ousman.model.Branch;
import com.ousman.model.Product;
import com.ousman.model.StockTransfer;
import com.ousman.repository.StockTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stock transfers between any two branches — Warehouse↔Warehouse,
 * Warehouse↔Store, or Store↔Store, since every location's stock lives on
 * a branch. Follows Request → Approve → Complete:
 *
 *   request()  — PENDING. No stock moves yet. Requester needs access to
 *                either the source or destination branch.
 *   approve()  — the moment stock actually leaves the source branch's
 *                batches and arrives as new batch(es) at the destination,
 *                preserving each source batch's original cost. Requires
 *                access to the SOURCE branch (releasing the stock).
 *   reject()   — closes the request with no stock movement. Same access
 *                requirement as approve.
 *   complete() — the destination branch confirms receipt, closing the
 *                loop for accountability. Requires access to the
 *                DESTINATION branch. No further stock movement happens
 *                here — that already happened at approval.
 */
@Service
@Transactional
public class StockTransferService {

    @Autowired
    private StockTransferRepository transferRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private AccessControlService accessControl;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StockTransfer> getPending() {
        return filterVisible(transferRepository.findAllPending());
    }

    @Transactional(readOnly = true)
    public List<StockTransfer> search(String status, Long branchId) {
        return filterVisible(transferRepository.search(status, branchId));
    }

    @Transactional(readOnly = true)
    public Page<StockTransfer> getPage(String status, Long branchId, Pageable pageable) {
        // Paged view is used from admin-facing screens only in practice;
        // scoped users use search()/getPending() which filter in-memory.
        return transferRepository.searchPage(status, branchId, pageable);
    }

    @Transactional(readOnly = true)
    public StockTransfer getById(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + id));
    }

    private List<StockTransfer> filterVisible(List<StockTransfer> all) {
        var visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return all;
        return all.stream()
                .filter(t -> visible.contains(t.getFromBranch().getId()) || visible.contains(t.getToBranch().getId()))
                .toList();
    }

    // ── Write: request ───────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public StockTransfer request(Long productId, Long fromBranchId, Long toBranchId, Integer quantity,
                                  Long batchId, String notes, String requestedBy) {
        if (fromBranchId.equals(toBranchId)) {
            throw new RuntimeException("Source and destination branches must be different.");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero.");
        }

        Product product = productService.getById(productId);
        Branch from = branchService.getById(fromBranchId);
        Branch to = branchService.getById(toBranchId);

        // Requester must have visibility into at least one side of the transfer.
        var visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible != null && !visible.contains(fromBranchId) && !visible.contains(toBranchId)) {
            throw new RuntimeException("You don't have access to either branch in this transfer.");
        }

        // Soft check now — the authoritative check happens again at approval,
        // since stock may have moved in the meantime.
        int available = batchService.getBranchStock(productId, fromBranchId);
        if (available < quantity) {
            throw new RuntimeException("Insufficient stock for '" + product.getName() + "' in " + from.getName() +
                    ". Available: " + available + ", requested: " + quantity);
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setProduct(product);
        transfer.setFromBranch(from);
        transfer.setToBranch(to);
        transfer.setQuantity(quantity);
        transfer.setBatchId(batchId);
        transfer.setNotes(notes);
        transfer.setRequestedBy(requestedBy != null ? requestedBy : "Admin");
        transfer.setStatus("PENDING");

        return transferRepository.saveAndFlush(transfer);
    }

    // ── Write: approve ───────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public StockTransfer approve(Long id, String approvedBy) {
        StockTransfer transfer = getById(id);
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new RuntimeException("Only pending transfers can be approved.");
        }
        accessControl.requireBranchAccess(transfer.getFromBranch().getId());

        transfer.setApprovedBy(approvedBy != null ? approvedBy : "Admin");
        transfer.setApprovedAt(LocalDateTime.now());
        transfer.setStatus("APPROVED");
        StockTransfer saved = transferRepository.saveAndFlush(transfer);

        // Moves the stock — source batches decrease, new destination batch(es) appear.
        batchService.consumeForTransfer(saved);

        // Audit trail on the product's overall history — the business-wide
        // total is unchanged (this only moves stock between branches), so
        // previousStock and newStock are both the current aggregate.
        Product product = productService.getById(saved.getProduct().getId());
        int currentStock = product.getStock();
        productService.recordHistory(product, -saved.getQuantity(), currentStock, currentStock,
                "TRANSFER_OUT", "Transfer to " + saved.getToBranch().getName() + " (transfer #" + saved.getId() + ")",
                saved.getApprovedBy(), "TRANSFER-" + saved.getId(), saved.getFromBranch());
        productService.recordHistory(product, saved.getQuantity(), currentStock, currentStock,
                "TRANSFER_IN", "Transfer from " + saved.getFromBranch().getName() + " (transfer #" + saved.getId() + ")",
                saved.getApprovedBy(), "TRANSFER-" + saved.getId(), saved.getToBranch());

        return saved;
    }

    // ── Write: reject ────────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public StockTransfer reject(Long id, String rejectedBy, String reason) {
        StockTransfer transfer = getById(id);
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new RuntimeException("Only pending transfers can be rejected.");
        }
        accessControl.requireBranchAccess(transfer.getFromBranch().getId());

        transfer.setRejectedBy(rejectedBy != null ? rejectedBy : "Admin");
        transfer.setRejectedAt(LocalDateTime.now());
        transfer.setRejectionReason(reason);
        transfer.setStatus("REJECTED");
        return transferRepository.saveAndFlush(transfer);
    }

    // ── Write: complete ───────────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public StockTransfer complete(Long id, String completedBy) {
        StockTransfer transfer = getById(id);
        if (!"APPROVED".equals(transfer.getStatus())) {
            throw new RuntimeException("Only approved transfers can be marked complete.");
        }
        accessControl.requireBranchAccess(transfer.getToBranch().getId());

        transfer.setCompletedBy(completedBy != null ? completedBy : "Admin");
        transfer.setCompletedAt(LocalDateTime.now());
        transfer.setStatus("COMPLETED");
        return transferRepository.saveAndFlush(transfer);
    }
}
