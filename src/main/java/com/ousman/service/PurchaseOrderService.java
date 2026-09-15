package com.ousman.service;

import com.ousman.model.*;
import com.ousman.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the purchase order lifecycle: DRAFT → ORDERED → (PARTIALLY_RECEIVED)
 * → RECEIVED, or CANCELLED at any point before anything's been received.
 *
 * A PO on its own never touches stock — only receiveLine() does, and it
 * does so by creating a ProductBatch via BatchService.receiveForPurchaseOrderLine,
 * so every batch created this way carries the exact cost agreed on the PO
 * line and is fully traceable back to the order and supplier it came from.
 */
@Service
@Transactional
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private PurchaseOrderLineRepository lineRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private AccessControlService accessControl;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PurchaseOrder> search(Long branchId, String status, String search) {
        return poRepository.search(branchId, status, search);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> getPage(Long branchId, String status, String search, Pageable pageable) {
        return poRepository.searchPage(branchId, status, search, pageable);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getById(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderLine> getLines(Long poId) {
        getById(poId); // 404 if missing
        return lineRepository.findByPurchaseOrderId(poId);
    }

    // ── Create ───────────────────────────────────────────────────────────

    public static class LineInput {
        public Long productId;
        public Integer orderedQuantity;
        public Double costPerUnit;
        public String notes;
    }

    public PurchaseOrder create(Long branchId, Long supplierId, LocalDate expectedDate, String notes,
                                 String createdBy, List<LineInput> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("A purchase order needs at least one product line.");
        }
        Branch branch = branchService.getById(branchId);
        accessControl.requireBranchAccess(branchId);
        Supplier supplier = supplierService.getById(supplierId);

        PurchaseOrder po = new PurchaseOrder();
        po.setBranch(branch);
        po.setSupplier(supplier);
        po.setStatus("DRAFT");
        po.setExpectedDate(expectedDate);
        po.setNotes(notes);
        po.setCreatedBy(createdBy != null ? createdBy : "Admin");

        PurchaseOrder saved = poRepository.saveAndFlush(po);
        if (saved.getOrderNumber() == null) {
            saved.setOrderNumber("PO-" + saved.getId());
            saved = poRepository.saveAndFlush(saved);
        }

        for (LineInput li : lines) {
            if (li.orderedQuantity == null || li.orderedQuantity <= 0) {
                throw new RuntimeException("Each line's ordered quantity must be greater than zero.");
            }
            if (li.costPerUnit == null || li.costPerUnit < 0) {
                throw new RuntimeException("Each line needs a cost per unit.");
            }
            Product product = productService.getById(li.productId);
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(saved);
            line.setProduct(product);
            line.setOrderedQuantity(li.orderedQuantity);
            line.setReceivedQuantity(0);
            line.setCostPerUnit(li.costPerUnit);
            line.setNotes(li.notes);
            lineRepository.saveAndFlush(line);
        }

        return saved;
    }

    // ── Status transitions ──────────────────────────────────────────────

    public PurchaseOrder markOrdered(Long id) {
        PurchaseOrder po = getById(id);
        accessControl.requireBranchAccess(po.getBranch().getId());
        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Only a draft order can be marked as ordered.");
        }
        po.setStatus("ORDERED");
        return poRepository.saveAndFlush(po);
    }

    public PurchaseOrder cancel(Long id) {
        PurchaseOrder po = getById(id);
        accessControl.requireBranchAccess(po.getBranch().getId());
        if ("RECEIVED".equals(po.getStatus()) || "CANCELLED".equals(po.getStatus())) {
            throw new RuntimeException("This order can't be cancelled — it's already " + po.getStatus().toLowerCase() + ".");
        }
        boolean anyReceived = getLines(id).stream().anyMatch(l -> l.getReceivedQuantity() != null && l.getReceivedQuantity() > 0);
        if (anyReceived) {
            throw new RuntimeException("This order already has receipts recorded against it and can't be cancelled.");
        }
        po.setStatus("CANCELLED");
        return poRepository.saveAndFlush(po);
    }

    // ── Receive against a line ───────────────────────────────────────────

    /**
     * Records a delivery against one line — creates a ProductBatch at the
     * PO's branch, for the exact cost agreed on that line, tagged back to
     * this order/line/supplier. Can be called more than once per line if a
     * delivery arrives in parts; the PO's overall status is recalculated
     * from all of its lines every time.
     */
    public ProductBatch receiveLine(Long poId, Long lineId, Integer quantity, String batchNumber,
                                     LocalDate receivedDate, String recordedBy) {
        PurchaseOrder po = getById(poId);
        accessControl.requireBranchAccess(po.getBranch().getId());
        if ("CANCELLED".equals(po.getStatus())) {
            throw new RuntimeException("This order is cancelled and can't receive stock.");
        }
        if ("DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Mark this order as ordered before receiving against it.");
        }
        PurchaseOrderLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Purchase order line not found: " + lineId));
        if (!line.getPurchaseOrder().getId().equals(poId)) {
            throw new RuntimeException("That line doesn't belong to this purchase order.");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity received must be greater than zero.");
        }
        int already = line.getReceivedQuantity() != null ? line.getReceivedQuantity() : 0;
        int remaining = line.getOrderedQuantity() - already;
        if (quantity > remaining) {
            throw new RuntimeException("Only " + remaining + " unit(s) remain on this line — can't receive " + quantity + ".");
        }

        ProductBatch batch = batchService.receiveForPurchaseOrderLine(
                line.getProduct().getId(), po.getBranch().getId(), quantity, line.getCostPerUnit(),
                batchNumber, receivedDate, po.getSupplier().getId(), po, line,
                "Received against " + po.getOrderNumber(), recordedBy);

        line.setReceivedQuantity(already + quantity);
        lineRepository.saveAndFlush(line);

        recalculateStatus(po);

        return batch;
    }

    private void recalculateStatus(PurchaseOrder po) {
        List<PurchaseOrderLine> lines = getLines(po.getId());
        boolean allFull = lines.stream().allMatch(l -> l.getReceivedQuantity() >= l.getOrderedQuantity());
        boolean anyReceived = lines.stream().anyMatch(l -> l.getReceivedQuantity() > 0);
        if (allFull) po.setStatus("RECEIVED");
        else if (anyReceived) po.setStatus("PARTIALLY_RECEIVED");
        poRepository.saveAndFlush(po);
    }
}
