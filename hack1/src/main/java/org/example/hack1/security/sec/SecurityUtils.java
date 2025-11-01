package org.example.hack1.security.sec;

import org.example.hack1.user.domain.User;
import org.example.hack1.user.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UserRepository userRepository;

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    public boolean isCentralUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_CENTRAL"));
    }

    public String getCurrentUserBranch() {
        String username = getCurrentUsername();
        if (username != null) {
            // Buscar el usuario en la base de datos para obtener su branch
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user != null) {
                return user.getBranch();
            }
        }
        return null;
    }

    public boolean hasAccessToBranch(String branch) {
        if (isCentralUser()) {
            return true; // Los usuarios CENTRAL tienen acceso a todas las branches
        }
        String userBranch = getCurrentUserBranch();
        return userBranch != null && userBranch.equals(branch);
    }

    public void validateBranchAccess(String branch) {
        if (!hasAccessToBranch(branch)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "No tienes acceso a la sucursal: " + branch
            );
        }
    }
}