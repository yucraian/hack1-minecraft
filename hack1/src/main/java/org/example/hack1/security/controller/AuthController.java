package controller;

import org.example.hack1.security.dto.AuthResponse;
import org.example.hack1.security.dto.LoginRequest;
import org.example.hack1.security.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.domain.UserRole;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import security.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Validar que branch sea obligatorio para BRANCH
        if (request.getUserRole() == UserRole.BRANCH &&
                (request.getBranch() == null || request.getBranch().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Branch es obligatorio para usuarios BRANCH");
        }

        // Validar que branch sea null para CENTRAL
        if (request.getUserRole() == UserRole.CENTRAL && request.getBranch() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Branch debe ser null para usuarios CENTRAL");
        }

        // Verificar si username ya existe
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username ya está en uso");
        }

        // Verificar si email ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email ya está en uso");
        }

        // Crear usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getUserRole());
        user.setBranch(request.getBranch());

        User savedUser = userRepository.save(user);

        // Response
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        response.put("branch", savedUser.getBranch());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String token = jwtUtil.generateToken(user);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setExpiresIn(3600L);
            response.setRole(user.getRole());
            response.setBranch(user.getBranch());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
    }
}