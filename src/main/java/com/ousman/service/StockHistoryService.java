package com.ousman.service;

import com.ousman.model.StockHistory;
import com.ousman.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Stock history is branch-scoped exactly like transfers: ADMIN and the
 * legacy WORKER role see every row; the three location-scoped roles
 * (WAREHOUSE_MANAGER, STORE_MANAGER, STAFF) only see rows tagged with a
 * branch they've been granted access to. Rows with no branch (legacy or
 * flat-stock manual adjustments) are only visible to unscoped roles —
 * there's no branch to check a scoped user's access against.
 */
@Service
@Transactional
public class StockHistoryService {

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private AccessControlService accessControl;

    @Transactional(readOnly = true)
    public List<StockHistory> getAll() {
        return getAllVisible(null);
    }

    /** All history visible to the current user, optionally narrowed to one branch. */
    @Transactional(readOnly = true)
    public List<StockHistory> getAllVisible(Long branchId) {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();

        if (branchId != null) {
            if (visible != null && !visible.contains(branchId)) {
                throw new RuntimeException("You don't have access to this branch.");
            }
            return stockHistoryRepository.findByBranchIdOrderedByDate(branchId);
        }

        if (visible == null) {
            return stockHistoryRepository.findAllOrderedByDate();
        }
        if (visible.isEmpty()) {
            return List.of();
        }
        return stockHistoryRepository.findByBranchIdInOrderedByDate(List.copyOf(visible));
    }

    @Transactional(readOnly = true)
    public List<StockHistory> getByProduct(Long productId) {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        List<StockHistory> all = stockHistoryRepository.findByProductId(productId);
        if (visible == null) return all;
        return all.stream()
                .filter(h -> h.getBranch() != null && visible.contains(h.getBranch().getId()))
                .toList();
    }

    public void delete(Long id) {
        StockHistory history = stockHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock history not found: " + id));
        if (history.getBranch() != null) {
            accessControl.requireBranchAccess(history.getBranch().getId());
        }
        stockHistoryRepository.delete(history);
    }
}
