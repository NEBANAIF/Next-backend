package com.ousman.service;

import com.ousman.model.Bank;
import com.ousman.repository.BankRepository;
import com.ousman.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BankService {

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Bank> getAll(String search) {
        return bankRepository.search(search);
    }

    /** Only active banks — used by the Sales form's bank picker. */
    @Transactional(readOnly = true)
    public List<Bank> getActive() {
        return bankRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Page<Bank> getPage(String search, Pageable pageable) {
        return bankRepository.searchPage(search, pageable);
    }

    @Transactional(readOnly = true)
    public Bank getById(Long id) {
        return bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found: " + id));
    }

    // ── Write ────────────────────────────────────────────────────────────

    public Bank create(Bank req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Bank name is required.");
        }
        String name = req.getName().trim();
        if (bankRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new RuntimeException("A bank named '" + name + "' already exists.");
        }
        req.setName(name);
        if (req.getActive() == null) req.setActive(true);
        return bankRepository.saveAndFlush(req);
    }

    public Bank update(Long id, Bank req) {
        Bank existing = getById(id);
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Bank name is required.");
        }
        String newName = req.getName().trim();
        bankRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new RuntimeException("A bank named '" + newName + "' already exists.");
            }
        });
        existing.setName(newName);
        existing.setAccountNumber(req.getAccountNumber());
        existing.setBranch(req.getBranch());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) existing.setActive(req.getActive());
        return bankRepository.saveAndFlush(existing);
    }

    /**
     * Hard-deletes a bank — but only if it has no payment history, since that
     * would orphan existing Payment rows. Banks with history should be
     * deactivated (active=false) via update() instead, which just hides them
     * from the Sales-form picker while preserving past records.
     */
    public void delete(Long id) {
        Bank bank = getById(id);
        if (paymentRepository.existsByBankId(id)) {
            throw new RuntimeException("This bank has linked payments and cannot be deleted. Deactivate it instead.");
        }
        bankRepository.delete(bank);
    }
}
