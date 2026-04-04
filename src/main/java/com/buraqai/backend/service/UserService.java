package com.buraqai.backend.service;

import com.buraqai.backend.dto.CreateUserRequestDTO;
import com.buraqai.backend.dto.UserResponseDTO;
import com.buraqai.backend.model.User;
import com.buraqai.backend.exception.UserAlreadyExistsException;
import com.buraqai.backend.model.UserRole;
import com.buraqai.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO createUser(CreateUserRequestDTO request) {

        // 1. Check if email is already taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        // 2. Build the new User entity
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.valueOf(request.getRole()));
        user.setActive(true);

        // 3. Save to PostgreSQL
        User savedUser = userRepository.save(user);

        // 4. Return safe response (no password)
        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getActive(),
                savedUser.getCreatedAt()
        );
    }
}