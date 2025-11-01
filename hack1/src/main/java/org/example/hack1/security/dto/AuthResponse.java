package org.example.hack1.security.dto;

import lombok.Data;
import org.example.hack1.user.domain.UserRole;

@Data
public class AuthResponse {
    private String token;
    private Long expiresIn;
    private UserRole userRole;
    private String branch;
}
