package org.example.hack1.user.domain;

import lombok.RequiredArgsConstructor;
import org.example.hack1.user.dto.UserRequestDto;
import org.example.hack1.user.dto.UserResponseDto;
import org.example.hack1.user.repo.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public UserResponseDto registerUser(UserRequestDto request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está en uso");
        }

        if (request.getUserRole() == UserRole.BRANCH &&
                (request.getBranch() == null || request.getBranch().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El branch es obligatorio para usuarios BRANCH");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);

        return modelMapper.map(saved, UserResponseDto.class);
    }

    public UserResponseDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return modelMapper.map(user, UserResponseDto.class);
    }
}
