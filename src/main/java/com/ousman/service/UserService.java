package com.ousman.service;

import com.ousman.model.User;
import com.ousman.model.UserBranchAccess;
import com.ousman.model.Branch;
import com.ousman.repository.UserRepository;
import com.ousman.repository.UserBranchAccessRepository;
import com.ousman.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository  userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService      jwtService;
    @Autowired private UserBranchAccessRepository branchAccessRepo;
    @Autowired private BranchRepository branchRepo;

    private static final int MIN_PASSWORD_LENGTH = 8;

    // ── Validate Helpers ──────────────────────────────────────────
    private static final java.util.Set<String> VALID_ROLES = java.util.Set.of(
        "ADMIN", "WORKER", "WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF"
    );

    private void validateRole(String role) {
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new RuntimeException("Invalid role. Must be one of: " + String.join(", ", VALID_ROLES) + ".");
        }
    }

    private void validateStatus(String status) {
        if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
            throw new RuntimeException("Invalid status. Must be ACTIVE or INACTIVE.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name cannot be empty.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    private boolean isPasswordHashed(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    // ── Login ──────────────────────────────────────────────────────
    @Transactional
    public String login(String email, String rawPassword) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Invalid email or password."));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is inactive. Contact admin.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid email or password.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepo.saveAndFlush(user);

        return jwtService.generateToken(user.getEmail(), user.getRole(), user.getName());
    }

    // ── Create User ────────────────────────────────────────────────
    @Transactional
    public User create(User req) {
        // Validate inputs
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required.");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new RuntimeException("Name is required.");
        }
        validatePassword(req.getPassword());

        if (userRepo.existsByEmail(req.getEmail().toLowerCase())) {
            throw new RuntimeException("Email already registered: " + req.getEmail());
        }

        req.setEmail(req.getEmail().toLowerCase());
        req.setName(req.getName().trim());
        req.setPassword(passwordEncoder.encode(req.getPassword()));
        
        if (req.getRole() == null) req.setRole("WORKER");
        else validateRole(req.getRole());
        
        if (req.getStatus() == null) req.setStatus("ACTIVE");
        else validateStatus(req.getStatus());

        // Every non-ADMIN account must be assigned to at least one branch at
        // creation time — an account with no branch has nowhere to work.
        // Only the three location-scoped roles are actually restricted by
        // branch (see AccessControlService.SCOPED_ROLES); ADMIN and the
        // legacy WORKER role keep their original unscoped visibility, so
        // this requirement must not apply to them.
        java.util.Set<String> scopedRoles = java.util.Set.of("WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF");
        boolean requiresBranch = scopedRoles.contains(req.getRole());
        List<Long> branchIds = req.getBranchIds();
        if (requiresBranch && (branchIds == null || branchIds.isEmpty())) {
            throw new RuntimeException("At least one branch must be assigned when creating this account.");
        }

        // Resolve every branch id up front so we fail before persisting the
        // user if any of them is invalid — never create a "half-set-up" account.
        List<Branch> branches = new java.util.ArrayList<>();
        if (branchIds != null) {
            for (Long branchId : branchIds) {
                branches.add(branchRepo.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId)));
            }
        }

        User saved = userRepo.saveAndFlush(req);

        for (Branch branch : branches) {
            UserBranchAccess access = new UserBranchAccess();
            access.setUser(saved);
            access.setBranch(branch);
            branchAccessRepo.saveAndFlush(access);
        }

        return saved;
    }

    // ── Get All Users ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepo.findAllByOrderByCreatedAtDesc();
    }

    // ── Get By ID ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    // ── Update User ────────────────────────────────────────────────
    @Transactional
    public User update(Long id, User req) {
        User existing = getById(id);

        // Validate name
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            validateName(req.getName());
            existing.setName(req.getName().trim());
        }

        // Validate and update email (check uniqueness if different)
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String newEmail = req.getEmail().toLowerCase();
            if (!existing.getEmail().equals(newEmail) && userRepo.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already in use: " + newEmail);
            }
            existing.setEmail(newEmail);
        }

        // Validate and update role
        if (req.getRole() != null) {
            validateRole(req.getRole());
            existing.setRole(req.getRole());
        }

        // Validate and update status
        if (req.getStatus() != null) {
            validateStatus(req.getStatus());
            existing.setStatus(req.getStatus());
        }

        // Update password if provided and not blank
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            validatePassword(req.getPassword());
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return userRepo.saveAndFlush(existing);
    }

    // ── Delete User ────────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepo.deleteById(id);
        userRepo.flush();
    }

    // ── Get By Email ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // ── Count Users ────────────────────────────────────────────────
    public long countUsers() {
        return userRepo.count();
    }

    // ── Save User (Internal Use Only) ──────────────────────────────
    public User save(User user) {
        // Only encode if password is plaintext (not already hashed)
        if (user.getPassword() != null && !isPasswordHashed(user.getPassword())) {
            validatePassword(user.getPassword());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepo.save(user);
    }

    // ── Find By Email ──────────────────────────────────────────────
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // ── Branch access (for the location-scoped roles) ───────────────
    @Transactional(readOnly = true)
    public List<UserBranchAccess> getBranchAccess(Long userId) {
        getById(userId); // 404 if the user doesn't exist
        return branchAccessRepo.findByUserId(userId);
    }

    @Transactional
    public UserBranchAccess assignBranch(Long userId, Long branchId) {
        User user = getById(userId);
        Branch branch = branchRepo.findById(branchId)
            .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId));
        return branchAccessRepo.findByUserIdAndBranchId(userId, branchId).orElseGet(() -> {
            UserBranchAccess access = new UserBranchAccess();
            access.setUser(user);
            access.setBranch(branch);
            return branchAccessRepo.saveAndFlush(access);
        });
    }

    @Transactional
    public void unassignBranch(Long userId, Long branchId) {
        if (!branchAccessRepo.existsByUserIdAndBranchId(userId, branchId)) {
            throw new RuntimeException("This user isn't assigned to that branch.");
        }
        branchAccessRepo.deleteByUserIdAndBranchId(userId, branchId);
    }
}