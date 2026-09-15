package com.ousman.service;

import com.ousman.model.Supplier;
import com.ousman.repository.PurchaseOrderRepository;
import com.ousman.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public List<Supplier> getAll(String search) {
        return supplierRepository.search(search);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getActive() {
        return supplierRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Page<Supplier> getPage(String search, Pageable pageable) {
        return supplierRepository.searchPage(search, pageable);
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
        String name = req.getName().trim();
        if (supplierRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new RuntimeException("A supplier named '" + name + "' already exists.");
        }
        req.setName(name);
        if (req.getActive() == null) req.setActive(true);
        return supplierRepository.saveAndFlush(req);
    }

    public Supplier update(Long id, Supplier req) {
        Supplier existing = getById(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            supplierRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new RuntimeException("A supplier named '" + newName + "' already exists.");
                }
            });
            existing.setName(newName);
        }
        existing.setContactName(req.getContactName());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setAddress(req.getAddress());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) existing.setActive(req.getActive());
        return supplierRepository.saveAndFlush(existing);
    }

    /** Only safe to delete a supplier with no purchase order history — deactivate otherwise. */
    public void delete(Long id) {
        Supplier supplier = getById(id);
        if (purchaseOrderRepository.existsBySupplierId(id)) {
            throw new RuntimeException("This supplier has purchase orders on record and cannot be deleted. Deactivate it instead.");
        }
        supplierRepository.delete(supplier);
    }
}
