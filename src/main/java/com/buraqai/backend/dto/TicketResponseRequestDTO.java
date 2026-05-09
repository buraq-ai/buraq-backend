package com.buraqai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketResponseRequestDTO {

    @NotBlank(message = "Response text cannot be empty")
    @Size(max = 2000, message = "Response text cannot exceed 2000 characters")
    private String responseText;

    // --- Getters and Setters ---

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }
}