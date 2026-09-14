package com.ousman.service;

import com.ousman.model.Branch;
import com.ousman.model.Location;
import com.ousman.repository.BranchRepository;
import com.ousman.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private BranchRepository branchRepository;

    // ── Read ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Location> getAll(String search) {
        return locationRepository.search(search);
    }

    @Transactional(readOnly = true)
    public List<Location> getActive() {
        return locationRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Location> getActiveByType(String type) {
        return locationRepository.findAllActiveByType(type);
    }

    @Transactional(readOnly = true)
    public Page<Location> getPage(String search, Pageable pageable) {
        return locationRepository.searchPage(search, pageable);
    }

    @Transactional(readOnly = true)
    public Location getById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
    }

    // ── Write ────────────────────────────────────────────────────────────

    /**
     * Creates a Location and immediately gives it a "Main" branch — the
     * default stock-holding unit every location needs from the moment it
     * exists, so receiving/selling/transferring can start right away
     * without a separate "now add a branch" step.
     */
    public Location create(Location req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Location name is required.");
        }
        String name = req.getName().trim();
        if (locationRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new RuntimeException("A location named '" + name + "' already exists.");
        }
        if (req.getType() == null || (!req.getType().equals("STORE") && !req.getType().equals("WAREHOUSE"))) {
            throw new RuntimeException("Location type must be STORE or WAREHOUSE.");
        }
        req.setName(name);
        if (req.getActive() == null) req.setActive(true);

        Location saved = locationRepository.saveAndFlush(req);

        Branch main = new Branch();
        main.setLocation(saved);
        main.setName("Main");
        main.setAddress(saved.getAddress());
        main.setPhone(saved.getPhone());
        main.setManagerName(saved.getManagerName());
        main.setIsDefault(true);
        main.setActive(true);
        branchRepository.saveAndFlush(main);

        return saved;
    }

    public Location update(Long id, Location req) {
        Location existing = getById(id);
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Location name is required.");
        }
        String newName = req.getName().trim();
        locationRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new RuntimeException("A location named '" + newName + "' already exists.");
            }
        });
        existing.setName(newName);
        existing.setAddress(req.getAddress());
        existing.setPhone(req.getPhone());
        existing.setManagerName(req.getManagerName());
        existing.setNotes(req.getNotes());
        // Type is intentionally not editable after creation — switching a
        // location between STORE and WAREHOUSE after it may already have
        // stock/sales/transfers tied to it would be misleading, not just a
        // label change.
        if (req.getActive() != null) existing.setActive(req.getActive());
        return locationRepository.saveAndFlush(existing);
    }

    /** Hard-deletes a location — only if it has no branches (a fresh location whose auto Main branch was itself removed, which BranchService never allows — so in practice this only fires for stale/empty rows). */
    public void delete(Long id) {
        Location location = getById(id);
        if (branchRepository.existsByLocationId(id)) {
            throw new RuntimeException("This location has branches under it and cannot be deleted. Deactivate it instead.");
        }
        locationRepository.delete(location);
    }
}
