package com.buraqai.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;   // e.g. "USER"

    private Long entityId;       // ID of the changed user

    private String action;       // e.g. "UPDATE"

    private String performedBy;  // email of the admin who made the change

    @Column(length = 1000)
    private String details;      // e.g. "Role changed from ROLE_EMPLOYEE to ROLE_ADMIN"

    @CreationTimestamp
    private LocalDateTime timestamp;

    // Getters and Setters

    public Long getId() { return id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
}