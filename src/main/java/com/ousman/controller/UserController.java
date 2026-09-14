package com.ousman.controller;

import com.ousman.model.User;
import com.ousman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserService userService;

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/users
    @PostMapping
    public ResponseEntity<?> create(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.create(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.update(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Branch access — which branches this user (a WAREHOUSE_MANAGER,
    //    STORE_MANAGER, or STAFF) can see and act on. Meaningless for
    //    ADMIN/WORKER, who stay unscoped regardless. Entire controller is
    //    already ADMIN-only via SecurityConfig ("/api/users/**").

    // GET /api/users/{id}/branches
    @GetMapping("/{id}/branches")
    public ResponseEntity<?> getBranchAccess(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getBranchAccess(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/users/{id}/branches/{branchId}
    @PostMapping("/{id}/branches/{branchId}")
    public ResponseEntity<?> assignBranch(@PathVariable Long id, @PathVariable Long branchId) {
        try {
            return ResponseEntity.ok(userService.assignBranch(id, branchId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/users/{id}/branches/{branchId}
    @DeleteMapping("/{id}/branches/{branchId}")
    public ResponseEntity<?> unassignBranch(@PathVariable Long id, @PathVariable Long branchId) {
        try {
            userService.unassignBranch(id, branchId);
            return ResponseEntity.ok(Map.of("message", "Branch unassigned"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}