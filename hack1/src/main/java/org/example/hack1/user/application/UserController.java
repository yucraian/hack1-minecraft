package org.example.hack1.user.application;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.domain.UserService;
import org.example.hack1.user.dto.UserRequestDto;
import org.example.hack1.user.dto.UserResponseDto;
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
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers()
                .stream()
                .map(this::mapToUserResponseDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToUserResponseDto(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        // Validaciones específicas para creación desde CENTRAL
        if (request.getRole() == org.example.hack1.user.domain.UserRole.BRANCH &&
                (request.getBranch() == null || request.getBranch().trim().isEmpty())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Branch es obligatorio para usuarios BRANCH");
        }

        if (request.getRole() == org.example.hack1.user.domain.UserRole.CENTRAL && request.getBranch() != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Branch debe ser null para usuarios CENTRAL");
        }

        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToUserResponseDto(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getUserRole())
                .branch(user.getBranch())
                .build();
    }
}