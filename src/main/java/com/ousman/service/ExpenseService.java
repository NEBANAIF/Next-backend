package com.ousman.service;

import com.ousman.model.Expense;
import com.ousman.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Expense service — read operations cached under "expenses",
 * write operations evict both "expenses" and "analytics" caches.
 *
 * Why "analytics" is also evicted on expense writes:
 *  Net profit = gross profit − expenses. When an expense is created
 *  or deleted, the cached analytics net profit figure is stale
 *  and must be recomputed from Neon on the next request.
 */
@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepo;

    // ── Read: cached ─────────────────────────────────────

    /**
     * Returns all expenses ordered newest first.
     * Cached under key "allExpenses" — evicted on any write.
     */
    @Cacheable(value = "expenses", key = "'allExpenses'")
    @Transactional(readOnly = true)
    public List<Expense> getAll() {
        return expenseRepo.findAllOrdered();
    }

    /**
     * Returns a single expense by ID.
     * Cached per ID.
     */
    @Cacheable(value = "expenses", key = "#id")
    @Transactional(readOnly = true)
    public Expense getById(Long id) {
        return expenseRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
    }

    // ── Write: cache-evicting ─────────────────────────────

    /**
     * Creates a new expense.
     * Evicts "expenses" cache and "analytics" cache (net profit changes).
     */
    @Caching(evict = {
        @CacheEvict(value = "expenses",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    @Transactional
    public Expense create(Expense req) {
        if (req.getCategory() == null || req.getCategory().isBlank()) {
            throw new RuntimeException("Category is required.");
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new RuntimeException("Amount must be greater than zero.");
        }
        return expenseRepo.saveAndFlush(req);
    }

    /**
     * Updates an existing expense.
     * Evicts "expenses" and "analytics" caches.
     */
    @Caching(evict = {
        @CacheEvict(value = "expenses",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    @Transactional
    public Expense update(Long id, Expense req) {
        Expense existing = expenseRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        existing.setCategory(req.getCategory());
        existing.setAmount(req.getAmount());
        existing.setDescription(req.getDescription());
        existing.setDate(req.getDate());
        return expenseRepo.saveAndFlush(existing);
    }

    /**
     * Deletes an expense by ID.
     * Evicts "expenses" and "analytics" caches.
     */
    @Caching(evict = {
        @CacheEvict(value = "expenses",  allEntries = true),
        @CacheEvict(value = "analytics", allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        if (!expenseRepo.existsById(id)) {
            throw new RuntimeException("Expense not found: " + id);
        }
        expenseRepo.deleteById(id);
        expenseRepo.flush();
    }
}
