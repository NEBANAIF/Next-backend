package com.ousman.service;

import com.ousman.model.Product;
import com.ousman.model.Sale;
import com.ousman.model.Branch;
import com.ousman.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sale service — read operations cached, write operations evict
 * both "products" and "analytics" caches.
 */
@Service
@Transactional
public class SaleService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private AccessControlService accessControl;

    @Autowired
    private CustomerService customerService;

    // ── Read: cached ──────────────────────────────────────────────────────

    @Cacheable(value = "products", key = "'allSales:' + (#branchId != null ? #branchId : 'ALL')")
    @Transactional(readOnly = true)
    public List<Sale> getAll(Long branchId) {
        return saleRepository.findAllOrderedByDate(branchId);
    }

    // Not cached, deliberately: the whole point of this method is that it
    // only ever touches one page of rows via the database's LIMIT/OFFSET,
    // so it's already cheap. Caching it would mean caching a separate
    // entry per (search, date, page, size) combination — an unbounded,
    // constantly-changing cache that costs more to maintain than the
    // query itself costs to run.
    @Transactional(readOnly = true)
    public Page<Sale> search(String search, LocalDate date, Long branchId, Pageable pageable) {
        return saleRepository.search(search, date, branchId, pageable);
    }

    // Queries only the requested date's rows (indexed column) instead of
    // loading every historical sale — used by /api/sales/today so that
    // endpoint stays fast as the sales table grows.
    @Cacheable(value = "products", key = "'salesByDate:' + #date + ':' + (#branchId != null ? #branchId : 'ALL')")
    @Transactional(readOnly = true)
    public List<Sale> getByDate(LocalDate date, Long branchId) {
        return branchId != null ? saleRepository.findBySaleDateAndBranchId(date, branchId)
                                 : saleRepository.findBySaleDate(date);
    }

    @Cacheable(value = "products", key = "'sale:' + #id")
    @Transactional(readOnly = true)
    public Sale getById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));
        if (sale.getBranch() != null) {
            accessControl.requireBranchAccess(sale.getBranch().getId());
        }
        return sale;
    }

    // ── Write: cache-evicting ─────────────────────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public Sale recordSale(Sale sale) {
        Product product = productService.getById(sale.getProduct().getId());

        // Resolve the branch (if one was supplied) to a fully-managed
        // entity up front, same as product — a nested {id: X} from the
        // frontend isn't enough on its own to save/query against.
        if (sale.getBranch() != null && sale.getBranch().getId() != null) {
            Branch branch = branchService.getById(sale.getBranch().getId());
            accessControl.requireBranchAccess(branch.getId());
            sale.setBranch(branch);
        } else {
            sale.setBranch(null);
        }

        // Optional link to a saved Customer (branch-scoped CRM). If given,
        // it also backfills customerName so every existing screen that
        // reads that field keeps working unchanged.
        if (sale.getCustomer() != null && sale.getCustomer().getId() != null) {
            com.ousman.model.Customer customer = customerService.getById(sale.getCustomer().getId());
            sale.setCustomer(customer);
            if (sale.getCustomerName() == null || sale.getCustomerName().isBlank()) {
                sale.setCustomerName(customer.getName());
            }
        } else {
            sale.setCustomer(null);
        }

        // ── Required: customer name ───────────────────────────────────────
        if (sale.getCustomerName() == null || sale.getCustomerName().isBlank()) {
            throw new RuntimeException("Customer name is required.");
        }

        // Validate sufficient stock before recording the sale
        if (product.getStock() < sale.getQuantity()) {
            throw new RuntimeException(
                "Insufficient stock for '" + product.getName() +
                "'. Available: " + product.getStock() +
                ", Requested: " + sale.getQuantity());
        }

        // Use product price if sale price not supplied by frontend
        if (sale.getPrice() == null) {
            sale.setPrice(product.getPrice());
        }

        // Calculate and set total revenue for this sale
        double total = sale.getPrice() * sale.getQuantity();
        sale.setTotal(total);
        sale.setProduct(product);

        // ── Payment validation ────────────────────────────────────────────
        String status = sale.getPaymentStatus();
        if (status == null || status.isBlank()) status = "PAID_FULL";
        sale.setPaymentStatus(status);

        if ("PAID_FULL".equals(status)) {
            sale.setPaidAmount(total);
            sale.setRemainingLoan(0.0);
        } else if ("PARTIAL_LOAN".equals(status)) {
            double paid = sale.getPaidAmount() != null ? sale.getPaidAmount() : 0.0;
            if (paid < 0)     throw new RuntimeException("Paid amount cannot be negative.");
            if (paid > total) throw new RuntimeException("Paid amount (" + paid + ") cannot exceed the total (" + total + ").");
            sale.setPaidAmount(paid);
            sale.setRemainingLoan(total - paid);
        } else {
            throw new RuntimeException("Invalid payment status: " + status);
        }

        // Persist the sale row
        Sale saved = saleRepository.save(sale);

        // Deduct stock and record the change in stock history
        int prevStock = product.getStock();
        int newStock  = prevStock - sale.getQuantity();
        product.setStock(newStock);

        productService.recordHistory(
            product,
            -sale.getQuantity(),
            prevStock, newStock,
            "SALE",
            "Sale to " + sale.getCustomerName(),
            sale.getRecordedBy() != null ? sale.getRecordedBy() : "Admin",
            "SALE-" + saved.getId(),
            sale.getBranch()
        );

        productService.update(product.getId(), product);

        // ── Batch consumption (only when a branch was specified) ───────
        // Draws down the actual batch(es) this sale came from — FIFO by
        // default, or one manually chosen batch — and prices the sale at
        // its real cost of goods instead of the product's flat cost. Sales
        // for products with no branch/batch history yet are left exactly
        // as before (costOfGoods stays null, Analytics falls back to
        // product.cost × quantity for those).
        if (sale.getBranch() != null) {
            BatchService.ConsumptionResult consumption = batchService.consumeForSale(
                saved, sale.getBranch().getId(), sale.getQuantity(), sale.getBatchId());
            saved.setCostOfGoods(consumption.totalCost);
            saved = saleRepository.save(saved);
        }

        // ── Auto-create the linked Payment record for whatever was collected
        //    right now (paidAmount — full total for PAID_FULL, the partial
        //    amount for PARTIAL_LOAN). Creates nothing if paidAmount is 0
        //    (e.g. a loan opened with nothing paid up front). Any validation
        //    failure here (bad bank, inactive bank, etc.) rolls back the
        //    whole transaction — the sale and stock deduction included —
        //    since this method is @Transactional.
        paymentService.createForSale(
            saved,
            sale.getPaymentMethod(),
            sale.getBankId(),
            sale.getTransactionRef(),
            sale.getPaymentNotes(),
            saved.getPaidAmount(),
            saved.getRecordedBy()
        );

        return saved;
    }

    /**
     * Partial update — only touches payment fields.
     * Called from the Loan page to record a repayment.
     * If paidAmount reaches total, paymentStatus flips to PAID_FULL.
     */
    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public Sale updateSalePayment(Long id, Double newPaidAmount) {
        return updateSalePayment(id, newPaidAmount, null, null, null, null, null);
    }

    /**
     * Same as above, but also records the loan repayment as a Payment row
     * (for whatever the paid amount actually increased by), with an
     * optional payment method / bank / transaction ref / notes. Called from
     * the Loans page when a customer pays down an outstanding balance.
     */
    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public Sale updateSalePayment(Long id, Double newPaidAmount, String paymentMethod, Long bankId,
                                   String transactionRef, String notes, String cashier) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));

        double total   = sale.getTotal()      != null ? sale.getTotal()      : 0.0;
        double oldPaid = sale.getPaidAmount() != null ? sale.getPaidAmount() : 0.0;

        if (newPaidAmount < 0)
            throw new RuntimeException("Paid amount cannot be negative.");
        if (newPaidAmount > total)
            throw new RuntimeException("Paid amount (" + newPaidAmount + ") cannot exceed the total (" + total + ").");

        double remaining = total - newPaidAmount;
        sale.setPaidAmount(newPaidAmount);
        sale.setRemainingLoan(remaining);
        sale.setPaymentStatus(remaining == 0.0 ? "PAID_FULL" : "PARTIAL_LOAN");

        Sale saved = saleRepository.save(sale);

        // Only the newly-collected portion becomes a Payment record — the
        // amount already paid at sale time was already logged then.
        double delta = newPaidAmount - oldPaid;
        if (delta > 0) {
            paymentService.createForSale(saved, paymentMethod, bankId, transactionRef, notes, delta, cashier);
        }

        return saved;
    }

    @Caching(evict = {
        @CacheEvict(value = "products",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    public void deleteSale(Long id) {
        Sale sale    = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));

        // Loan records (partially paid or otherwise carrying a balance) can never be
        // deleted, by any role — this is a hard business rule, not just a UI restriction.
        if ("PARTIAL_LOAN".equals(sale.getPaymentStatus()) || sale.getRemainingLoan() > 0.0) {
            throw new RuntimeException("Loan records cannot be deleted.");
        }

        Product product = sale.getProduct();

        int prevStock = product.getStock();
        int newStock  = prevStock + sale.getQuantity();
        product.setStock(newStock);

        productService.recordHistory(
            product,
            sale.getQuantity(),
            prevStock, newStock,
            "ADJUSTMENT",
            "Stock restored — sale #" + id + " deleted",
            "System",
            "SALE-DEL-" + id,
            sale.getBranch()
        );

        productService.update(product.getId(), product);

        // Restore whichever batch(es) this sale drew from, if any.
        if (sale.getBranch() != null) {
            batchService.restoreForSaleDeletion(id);
        }

        // Sale.sale_id on Payment is a non-nullable FK — clean up any linked
        // payment history before removing the sale row itself, or the delete
        // would fail (or leave orphaned rows, depending on the DB constraint).
        paymentService.deleteAllForSale(id);

        saleRepository.delete(sale);
    }
}
