package com.ousman.service;

import com.ousman.model.*;
import com.ousman.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Purchase Order → Receive → Batch workflow.
 *
 * Creating an order just records intent (what was ordered, from whom, into
 * which branch) — no stock moves yet. Receiving a line item (fully or
 * partially) is the moment stock actually appears: it calls
 * BatchService.receive() for exactly the quantity received, at that line's
 * unit cost, which creates a real ProductBatch and bumps Product.stock
 * through the same path a manual batch receipt would. A line can be
 * received across more than one delivery.
 */
@Service
@Transactional
public class PurchaseService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private AccessControlService accessControl;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> search(String status, Pageable pageable) {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return purchaseOrderRepository.search(status, pageable);
        if (visible.isEmpty()) return Page.empty(pageable);
        return purchaseOrderRepository.searchByBranches(List.copyOf(visible), status, pageable);
    }

    /** "Purchase History" sidebar item — every order with something received against it. */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getHistory() {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return purchaseOrderRepository.findHistory();
        if (visible.isEmpty()) return List.of();
        return purchaseOrderRepository.findHistoryByBranches(List.copyOf(visible));
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found: " + id));
        accessControl.requireBranchAccess(po.getBranch().getId());
        return po;
    }

    // ── Write: create ────────────────────────────────────────────────────

    public static class LineItemRequest {
        public Long productId;
        public Integer quantity;
        public Double unitCost;
    }

    public PurchaseOrder create(Long supplierId, Long branchId, List<LineItemRequest> lines,
                                 String notes, String orderedBy) {
        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("A purchase order needs at least one line item.");
        }
        Supplier supplier = supplierService.getById(supplierId);
        Branch branch = branchService.getById(branchId);
        accessControl.requireBranchAccess(branch.getId());

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setBranch(branch);
        po.setNotes(notes);
        po.setOrderedBy(orderedBy != null ? orderedBy : "Admin");
        po.setStatus("ORDERED");

        for (LineItemRequest line : lines) {
            if (line.quantity == null || line.quantity <= 0) {
                throw new RuntimeException("Every line item needs a quantity greater than zero.");
            }
            if (line.unitCost == null || line.unitCost < 0) {
                throw new RuntimeException("Every line item needs a non-negative unit cost.");
            }
            Product product = productService.getById(line.productId);
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantityOrdered(line.quantity);
            item.setQuantityReceived(0);
            item.setUnitCost(line.unitCost);
            po.getItems().add(item);
        }

        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(po);
        saved.setPoNumber("PO-" + saved.getId());
        return purchaseOrderRepository.saveAndFlush(saved);
    }

    // ── Write: receive ───────────────────────────────────────────────────

    /** Receives some or all of one line item — creates a real batch for exactly this quantity. */
    public PurchaseOrder receiveItem(Long purchaseOrderId, Long itemId, Integer quantity, String receivedBy) {
        PurchaseOrder po = getById(purchaseOrderId);
        if ("CANCELLED".equals(po.getStatus())) {
            throw new RuntimeException("This purchase order was cancelled.");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity received must be greater than zero.");
        }

        PurchaseOrderItem item = po.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Line item not found on this purchase order."));

        int remaining = item.getQuantityOrdered() - item.getQuantityReceived();
        if (quantity > remaining) {
            throw new RuntimeException("Only " + remaining + " unit(s) remain to be received on this line.");
        }

        // This is the actual stock-creating step — same path a manual batch
        // receipt uses, so everything downstream (Product.stock, Stock
        // History with branch+batch, dashboards) stays consistent.
        batchService.receive(
            item.getProduct().getId(), po.getBranch().getId(), quantity, item.getUnitCost(),
            null, LocalDate.now(), po.getSupplier().getName(),
            "Received against " + po.getPoNumber(), receivedBy);

        item.setQuantityReceived(item.getQuantityReceived() + quantity);
        recomputeStatus(po);
        return purchaseOrderRepository.saveAndFlush(po);
    }

    /** Receives every line item's full remaining quantity in one step. */
    public PurchaseOrder receiveAll(Long purchaseOrderId, String receivedBy) {
        PurchaseOrder po = getById(purchaseOrderId);
        if ("CANCELLED".equals(po.getStatus())) {
            throw new RuntimeException("This purchase order was cancelled.");
        }
        for (PurchaseOrderItem item : po.getItems()) {
            int remaining = item.getQuantityOrdered() - item.getQuantityReceived();
            if (remaining <= 0) continue;
            batchService.receive(
                item.getProduct().getId(), po.getBranch().getId(), remaining, item.getUnitCost(),
                null, LocalDate.now(), po.getSupplier().getName(),
                "Received against " + po.getPoNumber(), receivedBy);
            item.setQuantityReceived(item.getQuantityReceived() + remaining);
        }
        recomputeStatus(po);
        return purchaseOrderRepository.saveAndFlush(po);
    }

    private void recomputeStatus(PurchaseOrder po) {
        boolean allFull = po.getItems().stream()
                .allMatch(i -> i.getQuantityReceived() >= i.getQuantityOrdered());
        boolean anyReceived = po.getItems().stream()
                .anyMatch(i -> i.getQuantityReceived() > 0);
        po.setStatus(allFull ? "RECEIVED" : anyReceived ? "PARTIALLY_RECEIVED" : po.getStatus());
    }

    // ── Write: cancel ────────────────────────────────────────────────────

    public PurchaseOrder cancel(Long id, String cancelledBy) {
        PurchaseOrder po = getById(id);
        boolean anyReceived = po.getItems().stream().anyMatch(i -> i.getQuantityReceived() > 0);
        if (anyReceived) {
            throw new RuntimeException("This order already has stock received against it and can't be cancelled.");
        }
        po.setStatus("CANCELLED");
        po.setCancelledBy(cancelledBy != null ? cancelledBy : "Admin");
        po.setCancelledAt(java.time.LocalDateTime.now());
        return purchaseOrderRepository.saveAndFlush(po);
    }
}
