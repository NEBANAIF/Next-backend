package com.ousman.service;

import com.ousman.model.User;
import com.ousman.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Central place that answers "can this user see/act on this branch?".
 *
 * Every role except ADMIN is a branch user, scoped to exactly one branch —
 * the one assigned at account creation (User.branch, see UserService).
 * ADMIN has no branch and always sees everything. This is a strict,
 * single-branch model: there is no per-user multi-branch assignment
 * anymore (that was the old UserBranchAccess design).
 */
@Service
public class AccessControlService {

    @Autowired
    private UserRepository userRepository;

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
        return user.getRole() != null && !"ADMIN".equals(user.getRole());
    }

    /** Throws if the current user is a branch user and this isn't their branch. No-op for ADMIN. */
    @Transactional(readOnly = true)
    public void requireBranchAccess(Long branchId) {
        requireBranchAccess(currentUser(), branchId);
    }

    /** Same check, but against a User already resolved by the caller (avoids re-querying). */
    public void requireBranchAccess(User user, Long branchId) {
        if (!isScoped(user)) return;
        Long own = user.getBranch() != null ? user.getBranch().getId() : null;
        if (branchId == null || own == null || !own.equals(branchId)) {
            throw new RuntimeException("You don't have access to this branch.");
        }
    }

    /**
     * Null means "no restriction" (ADMIN — sees every branch). A non-null
     * set means the caller must filter results down to just these branch
     * ids — in practice always a single id for a branch user.
     */
    @Transactional(readOnly = true)
    public Set<Long> visibleBranchIdsOrNullForAll() {
        User user = currentUser();
        if (!isScoped(user)) return null;
        return user.getBranch() != null ? Set.of(user.getBranch().getId()) : Set.of();
    }

    /** The current user's own branch id, or null for ADMIN (no single branch). */
    @Transactional(readOnly = true)
    public Long currentBranchIdOrNull() {
        User user = currentUser();
        return user.getBranch() != null ? user.getBranch().getId() : null;
    }

    /**
     * Resolves the branch id a read query should actually filter by, given
     * what the caller requested. ADMIN gets exactly what they asked for
     * (including null = "every branch"). A branch user always gets their
     * own branch id back — regardless of what they passed in — so passing
     * someone else's branchId can never leak another branch's data; it's
     * silently overridden rather than rejected, since a read is not
     * something worth hard-failing over.
     */
    @Transactional(readOnly = true)
    public Long resolveBranchFilter(Long requestedBranchId) {
        User user = currentUser();
        if (!isScoped(user)) return requestedBranchId;
        return user.getBranch() != null ? user.getBranch().getId() : -1L; // -1L matches no real branch
    }
}
