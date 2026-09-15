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
    public List<Customer> search(Long branchId, String search) {
        return customerRepository.search(branchId, search);
    }

    @Transactional(readOnly = true)
    public List<Customer> getActiveForBranch(Long branchId) {
        accessControl.requireBranchAccess(branchId);
        return customerRepository.findAllActiveForBranch(branchId);
    }

    @Transactional(readOnly = true)
    public Page<Customer> getPage(Long branchId, String search, Pageable pageable) {
        return customerRepository.searchPage(branchId, search, pageable);
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    public Customer create(Customer req) {
        if (req.getBranch() == null || req.getBranch().getId() == null) {
            throw new RuntimeException("A branch is required for every customer.");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Customer name is required.");
        }
        Branch branch = branchService.getById(req.getBranch().getId());
        accessControl.requireBranchAccess(branch.getId());

        Customer customer = new Customer();
        customer.setBranch(branch);
        customer.setName(req.getName().trim());
        customer.setPhone(req.getPhone());
        customer.setEmail(req.getEmail());
        customer.setAddress(req.getAddress());
        customer.setNotes(req.getNotes());
        customer.setActive(req.getActive() == null || req.getActive());
        return customerRepository.saveAndFlush(customer);
    }

    public Customer update(Long id, Customer req) {
        Customer existing = getById(id);
        accessControl.requireBranchAccess(existing.getBranch().getId());
        if (req.getName() != null && !req.getName().isBlank()) existing.setName(req.getName().trim());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setAddress(req.getAddress());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) existing.setActive(req.getActive());
        return customerRepository.saveAndFlush(existing);
    }

    public void delete(Long id) {
        Customer existing = getById(id);
        accessControl.requireBranchAccess(existing.getBranch().getId());
        customerRepository.delete(existing);
    }
}
