package com.buraqai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateTicketStatusRequestDTO {

    @NotBlank(message = "Status is required")
    private String status;

    private String comment;

    // Default constructor (required for JSON deserialization)
    public UpdateTicketStatusRequestDTO() {}

    public UpdateTicketStatusRequestDTO(String status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}