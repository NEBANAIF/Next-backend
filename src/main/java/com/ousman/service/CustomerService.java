package com.ousman.service;

import com.ousman.model.Branch;
import com.ousman.model.Customer;
import com.ousman.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Customers are branch-scoped exactly like sales and stock history: ADMIN
 * sees everyone (optionally filtered to one branch), scoped roles only see
 * their own branch's customers.
 */
@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private AccessControlService accessControl;

    @Transactional(readOnly = true)
    public List<Customer> getAll(Long branchId) {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();

        if (branchId != null) {
            if (visible != null && !visible.contains(branchId)) {
                throw new RuntimeException("You don't have access to this branch.");
            }
            return customerRepository.findByBranchIdOrderByNameAsc(branchId);
        }
        if (visible == null) {
            return customerRepository.findAll();
        }
        if (visible.isEmpty()) return List.of();
        return customerRepository.findByBranchIdInOrderByNameAsc(List.copyOf(visible));
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(String search, Pageable pageable) {
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return customerRepository.search(search, pageable);
        if (visible.isEmpty()) return Page.empty(pageable);
        return customerRepository.searchByBranches(List.copyOf(visible), search, pageable);
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        accessControl.requireBranchAccess(c.getBranch().getId());
        return c;
    }

    public Customer create(Customer req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Customer name is required.");
        }
        if (req.getBranch() == null || req.getBranch().getId() == null) {
            throw new RuntimeException("A branch is required for every customer.");
        }
        Branch branch = branchService.getById(req.getBranch().getId());
        accessControl.requireBranchAccess(branch.getId());
        req.setBranch(branch);
        if (req.getActive() == null) req.setActive(true);
        return customerRepository.saveAndFlush(req);
    }

    public Customer update(Long id, Customer req) {
        Customer existing = getById(id); // also enforces branch access on the CURRENT branch

        if (req.getName() != null && !req.getName().isBlank()) existing.setName(req.getName().trim());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setAddress(req.getAddress());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) existing.setActive(req.getActive());

        // Allow re-assigning to a different branch, but only if the caller
        // has access to BOTH the old and the new branch.
        if (req.getBranch() != null && req.getBranch().getId() != null
                && !req.getBranch().getId().equals(existing.getBranch().getId())) {
            Branch newBranch = branchService.getById(req.getBranch().getId());
            accessControl.requireBranchAccess(newBranch.getId());
            existing.setBranch(newBranch);
        }

        return customerRepository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        Customer existing = getById(id); // enforces branch access
        customerRepository.delete(existing);
    }
}
