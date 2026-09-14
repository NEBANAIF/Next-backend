package com.ousman.service;

import com.ousman.model.Product;
import com.ousman.model.ProductBatch;
import com.ousman.model.Sale;
import com.ousman.model.SaleBatchAllocation;
import com.ousman.model.Branch;
import com.ousman.model.StockTransfer;
import com.ousman.model.StockTransferAllocation;
import com.ousman.repository.ProductBatchRepository;
import com.ousman.repository.SaleBatchAllocationRepository;
import com.ousman.repository.StockTransferAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns everything about batches: receiving stock into a batch, listing
 * available batches for a product+branch (FIFO order), and consuming
 * batches for a sale — either automatically (oldest first) or against one
 * manually chosen batch.
 *
 * Product.stock (the business-wide running total used by Dashboard,
 * low-stock alerts, and Analytics) is left completely alone here — callers
 * (SaleService, ProductService) keep updating it exactly as before. Batches
 * are an additional, finer-grained ledger layered underneath: per
 * branch, per cost. As long as every quantity change goes through both
 * layers, they stay in sync.
 */
@Service
@Transactional
public class BatchService {

    @Autowired
    private ProductBatchRepository batchRepository;

    @Autowired
    private SaleBatchAllocationRepository allocationRepository;

    @Autowired
    private StockTransferAllocationRepository transferAllocationRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BranchService branchService;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductBatch> getAvailable(Long productId, Long branchId) {
        return batchRepository.findAvailableFifo(productId, branchId);
    }

    @Transactional(readOnly = true)
    public List<ProductBatch> getAllForProductAndBranch(Long productId, Long branchId) {
        return batchRepository.findAllForProductAndBranch(productId, branchId);
    }

    @Transactional(readOnly = true)
    public Page<ProductBatch> getPage(Long productId, Long branchId, String search, Pageable pageable) {
        return batchRepository.searchPage(productId, branchId, search, pageable);
    }

    @Transactional(readOnly = true)
    public ProductBatch getById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + id));
    }

    /** Per-branch stock for a product, derived from open batches. */
    @Transactional(readOnly = true)
    public int getBranchStock(Long productId, Long branchId) {
        Integer sum = batchRepository.sumRemainingByProductAndBranch(productId, branchId);
        return sum != null ? sum : 0;
    }

    // ── Write: receive stock ─────────────────────────────────────────────

    /**
     * Receives a new batch of stock for a product into a branch, at its
     * own cost. Also bumps Product.stock (the business-wide total) via the
     * existing adjustStock path, so every screen that already reads
     * Product.stock keeps working unchanged.
     */
    public ProductBatch receive(Long productId, Long branchId, Integer quantity, Double costPerUnit,
                                 String batchNumber, LocalDate receivedDate,
                                 String supplier, String notes, String recordedBy) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity received must be greater than zero.");
        }
        if (costPerUnit == null || costPerUnit < 0) {
            throw new RuntimeException("Cost per unit is required and cannot be negative.");
        }
        Product product = productService.getById(productId);
        Branch branch = branchService.getById(branchId);

        ProductBatch batch = new ProductBatch();
        batch.setProduct(product);
        batch.setBranch(branch);
        batch.setBatchNumber((batchNumber == null || batchNumber.isBlank()) ? null : batchNumber.trim());
        batch.setCostPerUnit(costPerUnit);
        batch.setQuantityReceived(quantity);
        batch.setQuantityRemaining(quantity);
        batch.setReceivedDate(receivedDate != null ? receivedDate : LocalDate.now());
        batch.setSupplier(supplier);
        batch.setNotes(notes);
        batch.setRecordedBy(recordedBy != null ? recordedBy : "Admin");

        ProductBatch saved = batchRepository.saveAndFlush(batch);

        // Auto-generate a friendly batch number once we have an id, if none was supplied.
        if (saved.getBatchNumber() == null) {
            saved.setBatchNumber("BATCH-" + saved.getId());
            saved = batchRepository.saveAndFlush(saved);
        }

        String reason = "Batch received" + (saved.getBatchNumber() != null ? " (" + saved.getBatchNumber() + ")" : "")
                + " at " + branch.getName();
        productService.adjustStock(productId, quantity, reason, recordedBy != null ? recordedBy : "Admin",
                branch, saved);

        return saved;
    }

    // ── Write: edit non-quantity fields ──────────────────────────────────

    /** Corrects cost/notes/supplier on a batch. Quantities never change here. */
    public ProductBatch update(Long id, Double costPerUnit, String batchNumber,
                                String supplier, String notes) {
        ProductBatch batch = getById(id);
        if (costPerUnit != null && costPerUnit >= 0) batch.setCostPerUnit(costPerUnit);
        if (batchNumber != null && !batchNumber.isBlank()) batch.setBatchNumber(batchNumber.trim());
        batch.setSupplier(supplier);
        batch.setNotes(notes);
        return batchRepository.saveAndFlush(batch);
    }

    /** Only safe to delete a batch that's never been touched — nothing sold/discarded/transferred from it. */
    public void delete(Long id) {
        ProductBatch batch = getById(id);
        if (allocationRepository.existsByBatchId(id)) {
            throw new RuntimeException("This batch has sales recorded against it and cannot be deleted.");
        }
        if (!batch.getQuantityRemaining().equals(batch.getQuantityReceived())) {
            throw new RuntimeException("This batch has already been partially consumed and cannot be deleted.");
        }
        // Reverse the stock bump this batch caused when it was received.
        productService.adjustStock(batch.getProduct().getId(), -batch.getQuantityReceived(),
                "Batch deleted (" + (batch.getBatchNumber() != null ? batch.getBatchNumber() : "id " + id) + ")",
                "Admin", batch.getBranch(), batch);
        batchRepository.delete(batch);
    }

    // ── Write: consume batches for a sale ────────────────────────────────

    public static class ConsumptionResult {
        public double totalCost = 0.0;
        public List<SaleBatchAllocation> allocations = new ArrayList<>();
    }

    /**
     * Consumes `quantity` units of a product from a branch's batches for
     * the given (already-persisted) sale, either FIFO across as many
     * batches as needed, or entirely from one manually chosen batch.
     * Returns the resulting allocations plus the total cost of goods for
     * this sale line. Throws if the branch doesn't have enough batch
     * stock — this is a stricter, branch-scoped check on top of the
     * business-wide Product.stock check SaleService already does.
     */
    public ConsumptionResult consumeForSale(Sale sale, Long branchId, Integer quantity, Long manualBatchId) {
        ConsumptionResult result = new ConsumptionResult();
        Long productId = sale.getProduct().getId();

        if (manualBatchId != null) {
            ProductBatch batch = getById(manualBatchId);
            if (!batch.getProduct().getId().equals(productId)) {
                throw new RuntimeException("Selected batch does not belong to this product.");
            }
            if (!batch.getBranch().getId().equals(branchId)) {
                throw new RuntimeException("Selected batch does not belong to the selected branch.");
            }
            if (batch.getQuantityRemaining() < quantity) {
                throw new RuntimeException("Selected batch (" + batch.getBatchNumber() + ") only has "
                        + batch.getQuantityRemaining() + " units remaining — requested " + quantity + ".");
            }
            allocate(sale, batch, quantity, result);
            return result;
        }

        // Automatic FIFO — walk oldest-received batches first, spilling into
        // the next one if a single batch doesn't cover the full quantity.
        List<ProductBatch> available = batchRepository.findAvailableFifo(productId, branchId);
        int remaining = quantity;
        for (ProductBatch batch : available) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantityRemaining());
            allocate(sale, batch, take, result);
            remaining -= take;
        }
        if (remaining > 0) {
            Branch branch = branchService.getById(branchId);
            throw new RuntimeException("Insufficient batch stock for '" + sale.getProduct().getName() +
                    "' in " + branch.getName() + ". Short by " + remaining + " unit(s).");
        }
        return result;
    }

    private void allocate(Sale sale, ProductBatch batch, int qty, ConsumptionResult result) {
        batch.setQuantityRemaining(batch.getQuantityRemaining() - qty);
        batchRepository.save(batch);

        SaleBatchAllocation allocation = new SaleBatchAllocation();
        allocation.setSale(sale);
        allocation.setBatch(batch);
        allocation.setQuantity(qty);
        allocation.setCostPerUnit(batch.getCostPerUnit());
        allocationRepository.save(allocation);

        result.allocations.add(allocation);
        result.totalCost += qty * batch.getCostPerUnit();
    }

    /**
     * Reverses a sale's batch consumption (sale deletion) — restores each
     * allocated batch's remaining quantity and removes the allocation rows.
     */
    public void restoreForSaleDeletion(Long saleId) {
        List<SaleBatchAllocation> allocations = allocationRepository.findBySaleId(saleId);
        for (SaleBatchAllocation a : allocations) {
            ProductBatch batch = a.getBatch();
            batch.setQuantityRemaining(batch.getQuantityRemaining() + a.getQuantity());
            batchRepository.save(batch);
        }
        allocationRepository.deleteAll(allocations);
    }

    // ── Write: consume batches for an approved transfer ──────────────────

    public static class TransferResult {
        public List<StockTransferAllocation> allocations = new ArrayList<>();
    }

    /**
     * Called at transfer approval — the moment stock actually leaves the
     * source branch. Draws down source batch(es) exactly like a sale
     * (FIFO, or one manually chosen batch) but instead of "selling" the
     * quantity, creates a brand-new batch at the destination branch for
     * each portion consumed, carrying over the exact same cost per unit.
     * A transfer never changes what stock actually cost — it just moves it.
     */
    public TransferResult consumeForTransfer(StockTransfer transfer) {
        TransferResult result = new TransferResult();
        Long productId = transfer.getProduct().getId();
        Long fromBranchId = transfer.getFromBranch().getId();
        Integer quantity = transfer.getQuantity();
        Long manualBatchId = transfer.getBatchId();

        if (manualBatchId != null) {
            ProductBatch batch = getById(manualBatchId);
            if (!batch.getProduct().getId().equals(productId)) {
                throw new RuntimeException("Selected batch does not belong to this product.");
            }
            if (!batch.getBranch().getId().equals(fromBranchId)) {
                throw new RuntimeException("Selected batch does not belong to the source branch.");
            }
            if (batch.getQuantityRemaining() < quantity) {
                throw new RuntimeException("Selected batch (" + batch.getBatchNumber() + ") only has "
                        + batch.getQuantityRemaining() + " units remaining — requested " + quantity + ".");
            }
            allocateTransfer(transfer, batch, quantity, result);
            return result;
        }

        List<ProductBatch> available = batchRepository.findAvailableFifo(productId, fromBranchId);
        int remaining = quantity;
        for (ProductBatch batch : available) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantityRemaining());
            allocateTransfer(transfer, batch, take, result);
            remaining -= take;
        }
        if (remaining > 0) {
            throw new RuntimeException("Insufficient batch stock for '" + transfer.getProduct().getName() +
                    "' in " + transfer.getFromBranch().getName() + ". Short by " + remaining + " unit(s).");
        }
        return result;
    }

    private void allocateTransfer(StockTransfer transfer, ProductBatch source, int qty, TransferResult result) {
        source.setQuantityRemaining(source.getQuantityRemaining() - qty);
        batchRepository.save(source);

        ProductBatch dest = new ProductBatch();
        dest.setProduct(source.getProduct());
        dest.setBranch(transfer.getToBranch());
        dest.setCostPerUnit(source.getCostPerUnit());
        dest.setQuantityReceived(qty);
        dest.setQuantityRemaining(qty);
        dest.setReceivedDate(LocalDate.now());
        dest.setSupplier(source.getSupplier());
        dest.setNotes("Transferred from " + transfer.getFromBranch().getName() + " (source batch "
                + (source.getBatchNumber() != null ? source.getBatchNumber() : ("#" + source.getId())) + ")");
        dest.setRecordedBy(transfer.getApprovedBy() != null ? transfer.getApprovedBy() : "Admin");

        ProductBatch savedDest = batchRepository.saveAndFlush(dest);
        if (savedDest.getBatchNumber() == null) {
            savedDest.setBatchNumber("BATCH-" + savedDest.getId());
            savedDest = batchRepository.saveAndFlush(savedDest);
        }

        StockTransferAllocation allocation = new StockTransferAllocation();
        allocation.setTransfer(transfer);
        allocation.setSourceBatch(source);
        allocation.setDestinationBatch(savedDest);
        allocation.setQuantity(qty);
        allocation.setCostPerUnit(source.getCostPerUnit());
        transferAllocationRepository.save(allocation);

        result.allocations.add(allocation);
    }
}
