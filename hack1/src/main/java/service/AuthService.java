package service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse login(String username, String password) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Obtener usuario desde la base de datos
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            // Generar token JWT
            String token = jwtUtil.generateToken(user);

            // Crear response
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setExpiresIn(3600L);
            response.setRole(user.getRole());
            response.setBranch(user.getBranch());

            return response;

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
    }

    public User register(RegisterRequest request) {
        // Validar que branch sea obligatorio para BRANCH
        if (request.getRole() == Role.ROLE_BRANCH &&
                (request.getBranch() == null || request.getBranch().trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Branch es obligatorio para usuarios BRANCH");
        }

        // Validar que branch sea null para CENTRAL
        if (request.getRole() == Role.ROLE_CENTRAL && request.getBranch() != null) {
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
        user.setRole(request.getRole());
        user.setBranch(request.getBranch());

        return userRepository.save(user);
    }

    // Método para validar token (opcional)
    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}