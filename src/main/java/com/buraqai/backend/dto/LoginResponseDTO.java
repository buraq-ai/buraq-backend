package com.buraqai.backend.dto;

import com.buraqai.backend.model.UserRole;

public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private UserRole role;

    // Default constructor
    public LoginResponseDTO() {}

    // Constructor with fields
    public LoginResponseDTO(String accessToken, String refreshToken, UserRole role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}