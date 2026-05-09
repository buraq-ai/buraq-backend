package com.buraqai.backend.dto;

import com.buraqai.backend.model.TicketStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class TicketStatusHistoryDTO {

    private Long id;

    @JsonProperty("previous_status")
    private TicketStatus previousStatus;

    @JsonProperty("new_status")
    private TicketStatus newStatus;

    @JsonProperty("changed_by")
    private String changedBy;

    @JsonProperty("changed_at")
    private LocalDateTime changedAt;

    private String comment;

    // Default constructor (required for JSON deserialization)
    public TicketStatusHistoryDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TicketStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(TicketStatus previousStatus) { this.previousStatus = previousStatus; }

    public TicketStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TicketStatus newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}