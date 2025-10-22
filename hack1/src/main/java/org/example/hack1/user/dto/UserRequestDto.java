package org.example.hack1.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.hack1.user.domain.UserRole;

@Getter
@Setter
public class UserRequestDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 30, message = "El username debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "El username solo puede contener letras, números, '_' y '.'")
    private String username;

    @Email(message = "Debe ser un correo válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private UserRole userRole;

    private String branch;
}