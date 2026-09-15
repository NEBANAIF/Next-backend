package com.ousman.service;

import com.ousman.model.StockHistory;
import com.ousman.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockHistoryService {

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    public List<StockHistory> getAll(Long branchId) {
        return stockHistoryRepository.findAllOrderedByDate(branchId);
    }

    public List<StockHistory> getByProduct(Long productId) {
        return stockHistoryRepository.findByProductId(productId);
    }

    public void delete(Long id) {
        StockHistory history = stockHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock history not found: " + id));
        stockHistoryRepository.delete(history);
    }
}