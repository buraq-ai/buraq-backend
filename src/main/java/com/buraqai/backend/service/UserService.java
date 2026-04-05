package com.buraqai.backend.service;

import com.buraqai.backend.dto.CreateUserRequestDTO;
import com.buraqai.backend.dto.UpdateUserRequestDTO;
import com.buraqai.backend.dto.UserResponseDTO;
import com.buraqai.backend.exception.UserNotFoundException;
import com.buraqai.backend.model.AuditLog;
import com.buraqai.backend.model.User;
import com.buraqai.backend.exception.UserAlreadyExistsException;
import com.buraqai.backend.model.UserRole;
import com.buraqai.backend.repository.AuditLogRepository;
import com.buraqai.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class UserService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
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

    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO dto) {

        // 1. Find the user or throw 404
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        StringBuilder changes = new StringBuilder();

        // 2. Update fullName if provided
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            changes.append("Full name changed from '")
                    .append(user.getFullName())
                    .append("' to '")
                    .append(dto.getFullName())
                    .append("'. ");
            user.setFullName(dto.getFullName());
        }

        // 3. Update email if provided — must be unique
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!dto.getEmail().equals(user.getEmail())) {
                if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                    throw new UserAlreadyExistsException(dto.getEmail());
                }
                changes.append("Email changed from '")
                        .append(user.getEmail())
                        .append("' to '")
                        .append(dto.getEmail())
                        .append("'. ");
                user.setEmail(dto.getEmail());
            }
        }

        // 4. Update password if provided — hash it before saving
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            changes.append("Password was updated. ");
        }

        // 5. Update role if provided
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            changes.append("Role changed from '")
                    .append(user.getRole())
                    .append("' to '")
                    .append(dto.getRole())
                    .append("'. ");
            user.setRole(UserRole.valueOf(dto.getRole()));
        }

        // 6. Save updated user
        User savedUser = userRepository.save(user);

        // 7. Create audit log entry
        String adminEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        AuditLog log = new AuditLog();
        log.setEntityType("USER");
        log.setEntityId(id);
        log.setAction("UPDATE");
        log.setPerformedBy(adminEmail);
        log.setDetails(!changes.isEmpty() ? changes.toString() : "No changes detected.");
        auditLogRepository.save(log);

        // 8. Return updated user as DTO
        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getActive(),
                savedUser.getCreatedAt()
        );
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive(),
                user.getCreatedAt()
        );
    }
}