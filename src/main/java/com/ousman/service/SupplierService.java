package com.ousman.service;

import com.ousman.model.Supplier;
import com.ousman.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Suppliers are global — see Supplier.java for why. Same CRUD shape as ProductService. */
@Service
@Transactional
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Supplier> getActive() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Page<Supplier> search(String search, Pageable pageable) {
        return supplierRepository.search(search, pageable);
    }

    @Transactional(readOnly = true)
    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));
    }

    public Supplier create(Supplier req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Supplier name is required.");
        }
        if (req.getActive() == null) req.setActive(true);
        return supplierRepository.saveAndFlush(req);
    }

    public Supplier update(Long id, Supplier req) {
        Supplier existing = getById(id);
        if (req.getName() != null && !req.getName().isBlank()) existing.setName(req.getName().trim());
        existing.setContactPerson(req.getContactPerson());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setAddress(req.getAddress());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) existing.setActive(req.getActive());
        return supplierRepository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        Supplier existing = getById(id);
        supplierRepository.delete(existing);
    }
}
