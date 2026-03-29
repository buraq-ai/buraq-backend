package com.buraqai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}