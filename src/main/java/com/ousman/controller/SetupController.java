package com.ousman.controller;

import com.ousman.model.User;
import com.ousman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setup")
public class SetupController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            long userCount = userService.countUsers();
            boolean isInitialized = userCount > 0;
            return ResponseEntity.ok(new StatusResponse(isInitialized, userCount));
        } catch (Exception e) {
            return ResponseEntity.ok(new StatusResponse(false, 0));
        }
    }
    
    @PostMapping("/init-admin")
    public ResponseEntity<?> createFirstAdmin(@RequestBody AdminInitRequest request) {
        try {
            // Only allow creation when database is empty (no users exist)
            long userCount = userService.countUsers();
            if (userCount > 0) {
                return ResponseEntity.status(403).body(
                    new ErrorResponse("System already initialized. Cannot create admin user.")
                );
            }
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ErrorResponse("Email is required")
                );
            }
            
            if (request.getPassword() == null || request.getPassword().length() < 8) {
                return ResponseEntity.badRequest().body(
                    new ErrorResponse("Password must be at least 8 characters")
                );
            }
            
            User admin = new User();
            admin.setEmail(request.getEmail().toLowerCase());
            admin.setPassword(request.getPassword());
            admin.setName(request.getFullName() != null ? request.getFullName() : "Administrator");
            admin.setRole("ADMIN");
            
            User created = userService.save(admin);
            
            return ResponseEntity.status(201).body(
                new SuccessResponse(created.getId(), created.getEmail(), created.getRole())
            );
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                new ErrorResponse("Failed to create admin user: " + e.getMessage())
            );
        }
    }
    
    public static class StatusResponse {
        public boolean initialized;
        public long userCount;
        
        public StatusResponse(boolean initialized, long userCount) {
            this.initialized = initialized;
            this.userCount = userCount;
        }
        
        public boolean isInitialized() { return initialized; }
        public void setInitialized(boolean initialized) { this.initialized = initialized; }
        
        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
    }
    
    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class SuccessResponse {
        public String message = "Admin user created successfully";
        public Long userId;
        public String email;
        public String role;
        
        public SuccessResponse(Long userId, String email, String role) {
            this.userId = userId;
            this.email = email;
            this.role = role;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    
    public static class AdminInitRequest {
        private String email;
        private String password;
        private String fullName;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }
}