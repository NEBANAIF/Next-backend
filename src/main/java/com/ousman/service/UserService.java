package com.ousman.service;

import com.ousman.model.Branch;
import com.ousman.model.User;
import com.ousman.repository.BranchRepository;
import com.ousman.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired private UserRepository  userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService      jwtService;
    @Autowired private BranchRepository branchRepo;

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Set<String> VALID_ROLES = Set.of(
        "ADMIN", "WORKER", "WAREHOUSE_MANAGER", "STORE_MANAGER", "STAFF"
    );

    // ── Validate Helpers ──────────────────────────────────────────
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

    /** Every role except ADMIN is a branch user and must have exactly one branch. */
    private boolean requiresBranch(String role) {
        return !"ADMIN".equals(role);
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

        // Branch is mandatory for every role except ADMIN, and set exactly
        // once here at creation — the whole point of "branch users only see
        // their branch" is that this assignment isn't something they (or a
        // careless later edit) can silently drop.
        if (requiresBranch(req.getRole())) {
            if (req.getBranch() == null || req.getBranch().getId() == null) {
                throw new RuntimeException("A branch is required for every role except ADMIN.");
            }
            Branch branch = branchRepo.findById(req.getBranch().getId())
                .orElseThrow(() -> new RuntimeException("Branch not found: " + req.getBranch().getId()));
            req.setBranch(branch);
        } else {
            req.setBranch(null); // ADMIN is never branch-scoped
        }

        return userRepo.saveAndFlush(req);
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

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            validateName(req.getName());
            existing.setName(req.getName().trim());
        }

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            String newEmail = req.getEmail().toLowerCase();
            if (!existing.getEmail().equals(newEmail) && userRepo.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already in use: " + newEmail);
            }
            existing.setEmail(newEmail);
        }

        // Role and branch are validated together: a role change that drops
        // to/from ADMIN changes whether a branch is required at all.
        String effectiveRole = existing.getRole();
        if (req.getRole() != null) {
            validateRole(req.getRole());
            effectiveRole = req.getRole();
            existing.setRole(effectiveRole);
        }

        if (requiresBranch(effectiveRole)) {
            Long newBranchId = req.getBranch() != null ? req.getBranch().getId() : null;
            if (newBranchId == null && existing.getBranch() != null) {
                // Role/branch unchanged from a prior valid state — keep it.
            } else if (newBranchId == null) {
                throw new RuntimeException("A branch is required for every role except ADMIN.");
            } else {
                Branch branch = branchRepo.findById(newBranchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + newBranchId));
                existing.setBranch(branch);
            }
        } else {
            existing.setBranch(null); // ADMIN is never branch-scoped
        }

        if (req.getStatus() != null) {
            validateStatus(req.getStatus());
            existing.setStatus(req.getStatus());
        }

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
}
