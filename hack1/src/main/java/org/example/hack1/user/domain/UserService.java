package org.example.hack1.user.domain;

import lombok.RequiredArgsConstructor;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User userRequest) {
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está en uso");
        }

        if (userRequest.getUserRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        if (userRequest.getUserRole().equals(UserRole.BRANCH) && (userRequest.getBranch() == null || userRequest.getBranch().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El branch es obligatorio para usuarios BRANCH");
        }

        userRequest.setPassword(passwordEncoder.encode(userRequest.getPassword()));

        return userRepository.save(userRequest);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}