package security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    public String getCurrentUserBranch() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            return user.getBranch();
        }
        return null;
    }

    public boolean isCentralUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CENTRAL"));
    }

    public boolean hasAccessToBranch(String branch) {
        if (isCentralUser()) {
            return true;
        }
        String userBranch = getCurrentUserBranch();
        return userBranch != null && userBranch.equals(branch);
    }
}