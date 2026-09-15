package com.ousman.service;

import com.ousman.model.Product;
import com.ousman.model.Return;
import com.ousman.model.Sale;
import com.ousman.repository.ReturnRepository;
import com.ousman.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A return is always against a specific past sale. Restocking (restock =
 * true) credits back the exact batch(es) the sale drew from and bumps
 * Product.stock the same way receiving stock does; restock = false records
 * the return (for refund/reporting) with no stock movement at all.
 */
@Service
@Transactional
public class ReturnService {

    @Autowired
    private ReturnRepository returnRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private AccessControlService accessControl;

    @Transactional(readOnly = true)
    public List<Return> search(Long branchId) {
        return returnRepository.search(branchId);
    }

    @Transactional(readOnly = true)
    public Page<Return> getPage(Long branchId, Pageable pageable) {
        return returnRepository.searchPage(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Return getById(Long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Return> getForSale(Long saleId) {
        return returnRepository.findBySaleId(saleId);
    }

    public Return create(Long saleId, Integer quantity, String reason, Boolean restock,
                          Double refundAmount, String processedBy) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Return quantity must be greater than zero.");
        }
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + saleId));

        if (sale.getBranch() != null) {
            accessControl.requireBranchAccess(sale.getBranch().getId());
        }

        int alreadyReturned = returnRepository.sumReturnedQuantityForSale(saleId);
        int stillReturnable = sale.getQuantity() - alreadyReturned;
        if (quantity > stillReturnable) {
            throw new RuntimeException("Only " + stillReturnable + " unit(s) of this sale can still be returned.");
        }

        boolean doRestock = restock == null || restock;

        if (doRestock) {
            Product product = sale.getProduct();
            int prevStock = product.getStock();
            int newStock = prevStock + quantity;
            product.setStock(newStock);

            productService.recordHistory(
                product,
                quantity,
                prevStock, newStock,
                "RETURN",
                "Return — sale #" + saleId + (reason != null && !reason.isBlank() ? " (" + reason + ")" : ""),
                processedBy != null ? processedBy : "Admin",
                "RETURN-SALE-" + saleId,
                sale.getBranch()
            );
            productService.update(product.getId(), product);

            if (sale.getBranch() != null) {
                batchService.restoreForReturn(saleId, quantity);
            }
        }

        Return ret = new Return();
        ret.setSale(sale);
        ret.setProduct(sale.getProduct());
        ret.setBranch(sale.getBranch());
        ret.setQuantity(quantity);
        ret.setReason(reason);
        ret.setRestock(doRestock);
        ret.setRefundAmount(refundAmount);
        ret.setProcessedBy(processedBy != null ? processedBy : "Admin");

        return returnRepository.saveAndFlush(ret);
    }
}
