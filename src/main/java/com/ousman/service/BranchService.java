package com.ousman.service;

import com.ousman.model.Branch;
import com.ousman.model.Location;
import com.ousman.repository.BranchRepository;
import com.ousman.repository.LocationRepository;
import com.ousman.repository.ProductBatchRepository;
import com.ousman.repository.StockTransferRepository;
import com.ousman.repository.UserBranchAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ProductBatchRepository batchRepository;

    @Autowired
    private StockTransferRepository transferRepository;

    @Autowired
    private UserBranchAccessRepository accessRepository;

    @Autowired
    private AccessControlService accessControl;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Branch> getAll(Long locationId, String search) {
        return branchRepository.search(locationId, search);
    }

    @Transactional(readOnly = true)
    public List<Branch> getByLocation(Long locationId) {
        return branchRepository.findByLocationId(locationId);
    }

    /** Active branches, scoped to the current user's assigned branches unless they're ADMIN/WORKER. */
    @Transactional(readOnly = true)
    public List<Branch> getActive() {
        List<Branch> all = branchRepository.findAllActive();
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return all;
        return all.stream().filter(b -> visible.contains(b.getId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Branch> getActiveByLocationType(String locationType) {
        List<Branch> all = branchRepository.findAllActiveByLocationType(locationType);
        Set<Long> visible = accessControl.visibleBranchIdsOrNullForAll();
        if (visible == null) return all;
        return all.stream().filter(b -> visible.contains(b.getId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Branch> getPage(Long locationId, String search, Pageable pageable) {
        return branchRepository.searchPage(locationId, search, pageable);
    }

    @Transactional(readOnly = true)
    public Branch getById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + id));
    }

    // ── Write ────────────────────────────────────────────────────────────

    /** Adds another branch under an existing Location — the auto "Main" branch is created only by LocationService. */
    public Branch create(Branch req) {
        if (req.getLocation() == null || req.getLocation().getId() == null) {
            throw new RuntimeException("A location is required for every branch.");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Branch name is required.");
        }
        Location location = locationRepository.findById(req.getLocation().getId())
                .orElseThrow(() -> new RuntimeException("Location not found: " + req.getLocation().getId()));

        Branch branch = new Branch();
        branch.setLocation(location);
        branch.setName(req.getName().trim());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setManagerName(req.getManagerName());
        branch.setNotes(req.getNotes());
        branch.setIsDefault(false);
        branch.setActive(req.getActive() == null || req.getActive());
        return branchRepository.saveAndFlush(branch);
    }

    public Branch update(Long id, Branch req) {
        Branch existing = getById(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            existing.setName(req.getName().trim());
        }
        existing.setAddress(req.getAddress());
        existing.setPhone(req.getPhone());
        existing.setManagerName(req.getManagerName());
        existing.setNotes(req.getNotes());
        if (req.getActive() != null) {
            if (existing.getIsDefault() && !req.getActive()) {
                throw new RuntimeException("The Main branch of a location can't be deactivated — deactivate the location instead.");
            }
            existing.setActive(req.getActive());
        }
        return branchRepository.saveAndFlush(existing);
    }

    /** Only safe to delete a branch that's never held stock, been sold from, or been part of a transfer. The default "Main" branch of a location can never be deleted. */
    public void delete(Long id) {
        Branch branch = getById(id);
        if (Boolean.TRUE.equals(branch.getIsDefault())) {
            throw new RuntimeException("The Main branch of a location can't be deleted.");
        }
        if (hasAnyBatchHistory(id)) {
            throw new RuntimeException("This branch has batch/stock history and cannot be deleted. Deactivate it instead.");
        }
        if (transferRepository.existsByFromBranchIdOrToBranchId(id, id)) {
            throw new RuntimeException("This branch has stock transfers on record and cannot be deleted. Deactivate it instead.");
        }
        if (accessRepository.existsByBranchId(id)) {
            throw new RuntimeException("This branch is assigned to one or more users. Unassign it first, or deactivate the branch instead.");
        }
        branchRepository.delete(branch);
    }

    private boolean hasAnyBatchHistory(Long branchId) {
        // Any batch ever created for this branch (received or fully consumed) blocks deletion —
        // history should be preserved via deactivation, not silently lost.
        return !batchRepository.searchPage(null, branchId, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).isEmpty();
    }
}
