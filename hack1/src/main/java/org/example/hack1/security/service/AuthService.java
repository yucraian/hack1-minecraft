package org.example.hack1.security.service;

import lombok.RequiredArgsConstructor;
import org.example.hack1.security.dto.AuthResponse;
import org.example.hack1.security.dto.RegisterRequest;
import org.example.hack1.security.sec.JwtUtil;
import org.example.hack1.user.domain.User;
import org.example.hack1.user.domain.UserRole;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // 🟢 LOGIN
    public AuthResponse login(String username, String password) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Buscar usuario en la base de datos
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            // Generar token JWT
            String token = jwtUtil.generateToken(user);

            // Crear respuesta - CORREGIDO
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setExpiresIn(3600L);
            response.setUserRole(user.getUserRole());  // Corregido: setUserRole()
            response.setBranch(user.getBranch());

            return response;

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
    }

    // 🟡 REGISTER
    public User register(RegisterRequest request) {
        if (request.getUserRole() == UserRole.BRANCH &&
                (request.getBranch() == null || request.getBranch().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Branch es obligatorio para usuarios BRANCH");
        }

        if (request.getUserRole() == UserRole.CENTRAL && request.getBranch() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Branch debe ser null para usuarios CENTRAL");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username ya está en uso");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya está en uso");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserRole(request.getUserRole());
        user.setBranch(request.getBranch());

        return userRepository.save(user);
    }

    // 🧩 VALIDAR TOKEN
    public boolean validateToken(String token, User user) {
        try {
            return jwtUtil.validateToken(token, user);
        } catch (Exception e) {
            return false;
        }
    }
}