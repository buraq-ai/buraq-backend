package com.buraqai.backend.dto;

import com.buraqai.backend.model.TicketStatus;
import com.buraqai.backend.model.TicketSource;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class TicketResponseDTO {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketSource source;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("assigned_to")
    private String assignedTo;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Default constructor (required for JSON deserialization)
    public TicketResponseDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketSource getSource() { return source; }
    public void setSource(TicketSource source) { this.source = source; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}