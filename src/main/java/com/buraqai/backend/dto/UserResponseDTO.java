package com.buraqai.backend.dto;

import java.time.LocalDateTime;

public class UserResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;

    // Constructor
    public UserResponseDTO(Long id, String fullName, String email, String role, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}