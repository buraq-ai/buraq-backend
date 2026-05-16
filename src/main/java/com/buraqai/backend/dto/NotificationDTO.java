package com.buraqai.backend.dto;

import com.buraqai.backend.model.Notification;
import com.buraqai.backend.model.NotificationType;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private Long ticketId;
    private LocalDateTime createdAt;

    // Constructors

    public NotificationDTO() {
    }

    public NotificationDTO(Long id, String title, String message,
                           NotificationType type, Boolean isRead,
                           Long ticketId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.ticketId = ticketId;
        this.createdAt = createdAt;
    }

    // Static factory method to convert from entity to DTO
    public static NotificationDTO fromEntity(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getTicketId(),
                notification.getCreatedAt()
        );
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}