package com.buraqai.backend.repository;

import com.buraqai.backend.model.User;
import com.buraqai.backend.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Search by name OR email (case-insensitive) with pagination
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String email,
            Pageable pageable
    );

    // Filter by role only with pagination
    Page<User> findByRole(
            UserRole role,
            Pageable pageable
    );

    // Filter by active status only with pagination
    Page<User> findByActive(
            Boolean active,
            Pageable pageable
    );

    // Combined: search by name/email AND filter by role AND filter by status
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndRoleAndActive(
            String fullName,
            String email,
            UserRole role,
            Boolean active,
            Pageable pageable
    );

    // Search + Role combined
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndRole(
            String fullName,
            String email,
            UserRole role,
            Pageable pageable
    );

    // Search + Active combined
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndActive(
            String fullName,
            String email,
            Boolean active,
            Pageable pageable
    );

    // Role + Active combined
    Page<User> findByRoleAndActive(
            UserRole role,
            Boolean active,
            Pageable pageable
    );
}