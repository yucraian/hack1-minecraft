package org.example.hack1.user.application;


import lombok.RequiredArgsConstructor;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.domain.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getUserRole())
                .branch(user.getBranch())
                .build();
    }

    // DTO para response
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private org.example.hack1.user.domain.UserRole role;
        private String branch;

        // Builder, Getters y Setters
        public static UserResponseBuilder builder() {
            return new UserResponseBuilder();
        }

        public static class UserResponseBuilder {
            private Long id;
            private String username;
            private String email;
            private org.example.hack1.user.domain.UserRole role;
            private String branch;

            public UserResponseBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public UserResponseBuilder username(String username) {
                this.username = username;
                return this;
            }

            public UserResponseBuilder email(String email) {
                this.email = email;
                return this;
            }

            public UserResponseBuilder role(org.example.hack1.user.domain.UserRole role) {
                this.role = role;
                return this;
            }

            public UserResponseBuilder branch(String branch) {
                this.branch = branch;
                return this;
            }

            public UserResponse build() {
                UserResponse response = new UserResponse();
                response.id = this.id;
                response.username = this.username;
                response.email = this.email;
                response.role = this.role;
                response.branch = this.branch;
                return response;
            }
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public org.example.hack1.user.domain.UserRole getRole() { return role; }
        public String getBranch() { return branch; }
    }
}