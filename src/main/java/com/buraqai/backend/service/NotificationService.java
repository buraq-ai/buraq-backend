package com.buraqai.backend.service;

import com.buraqai.backend.dto.NotificationDTO;
import com.buraqai.backend.model.Notification;
import com.buraqai.backend.model.NotificationType;
import com.buraqai.backend.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Constructor injection (preferred over @Autowired field injection)
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Creates and saves a notification to PostgreSQL.
     * This is the primary delivery method — in-app notification.
     * Email sending will be added later as a secondary method.
     */
    public NotificationDTO createNotification(String recipientEmail, String title,
                                              String message, NotificationType type,
                                              Long ticketId) {
        Notification notification = new Notification(
                recipientEmail, title, message, type, ticketId);
        Notification saved = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(saved);
    }

    /**
     * Returns the last 20 unread notifications for the authenticated user,
     * ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(String email) {
        return notificationRepository
                .findByRecipientEmailAndIsReadFalseOrderByCreatedAtDesc(email)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns the last 20 notifications (both read and unread) for the user,
     * ordered by newest first. Used by the "View All" feature in the frontend.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getRecentNotifications(String email) {
        return notificationRepository
                .findByRecipientEmailOrderByCreatedAtDesc(
                        email, PageRequest.of(0, 20))
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns the count of unread notifications for the badge on the bell icon.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email);
    }

    /**
     * Marks a single notification as read.
     * Validates ownership: only the recipient can mark their own notification.
     * Returns true if successful, false if notification not found or wrong owner.
     */
    public boolean markAsRead(Long notificationId, String email) {
        int updatedRows = notificationRepository.markAsRead(notificationId, email);
        return updatedRows > 0;
    }

    /**
     * Marks all unread notifications as read for the given user.
     * Returns the number of notifications that were marked as read.
     */
    public int markAllAsRead(String email) {
        return notificationRepository.markAllAsRead(email);
    }
}