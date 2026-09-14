package com.ousman.service;

import com.ousman.model.User;
import com.ousman.model.UserBranchAccess;
import com.ousman.repository.UserBranchAccessRepository;
import com.ousman.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central place that answers "can this user see/act on this branch?".
 *
 * Only the three new location-scoped roles are actually restricted:
 * WAREHOUSE_MANAGER, STORE_MANAGER, STAFF — each limited to the branches
 * explicitly assigned via UserBranchAccess. ADMIN always sees everything.
 * The legacy WORKER role also keeps full, unscoped visibility — it existed
 * before branches did, and re-scoping it retroactively would silently lock
 * existing worker accounts out of data they could already see.
 */
@Service
public class AccessControlService {

    // Roles whose visibility is limited to their assigned branches.
    private static final Set<String> SCOPED_ROLES = Set.of("WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBranchAccessRepository accessRepository;

    @Transactional(readOnly = true)
    public User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
    }

    public boolean isScoped(User user) {
        return user.getRole() != null && SCOPED_ROLES.contains(user.getRole());
    }

    /** Every branch id this user is explicitly allowed on. Meaningless (and unused) for non-scoped roles. */
    @Transactional(readOnly = true)
    public Set<Long> accessibleBranchIds(User user) {
        return accessRepository.findByUserId(user.getId()).stream()
                .map(a -> a.getBranch().getId())
                .collect(Collectors.toSet());
    }

    /** Throws if the current user is scoped and doesn't have explicit access to this branch. No-op for ADMIN/WORKER. */
    @Transactional(readOnly = true)
    public void requireBranchAccess(Long branchId) {
        User user = currentUser();
        if (!isScoped(user)) return;
        if (branchId == null || !accessRepository.existsByUserIdAndBranchId(user.getId(), branchId)) {
            throw new RuntimeException("You don't have access to this branch.");
        }
    }

    /** Same check, but against a User already resolved by the caller (avoids re-querying). */
    @Transactional(readOnly = true)
    public void requireBranchAccess(User user, Long branchId) {
        if (!isScoped(user)) return;
        if (branchId == null || !accessRepository.existsByUserIdAndBranchId(user.getId(), branchId)) {
            throw new RuntimeException("You don't have access to this branch.");
        }
    }

    /**
     * Null means "no restriction" (ADMIN/WORKER) — callers should treat a
     * null return as "don't filter". A non-null (possibly empty) set means
     * the caller must filter results down to just these branch ids.
     */
    @Transactional(readOnly = true)
    public Set<Long> visibleBranchIdsOrNullForAll() {
        User user = currentUser();
        if (!isScoped(user)) return null;
        return accessibleBranchIds(user);
    }

    @Transactional(readOnly = true)
    public List<UserBranchAccess> getAccessForUser(Long userId) {
        return accessRepository.findByUserId(userId);
    }
}
