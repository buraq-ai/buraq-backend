package com.buraqai.backend.controller;

import com.buraqai.backend.dto.NotificationDTO;
import com.buraqai.backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get the email of the currently authenticated user from the JWT token.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Get the last 20 notifications (read and unread) for the authenticated user,
     * ordered by newest first.
     * Used by the notification bell dropdown and "View All" page.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        String currentUserEmail = getCurrentUserEmail();

        List<NotificationDTO> notifications = notificationService.getRecentNotifications(currentUserEmail);

        logger.info("Notifications retrieved | user={} | count={}", currentUserEmail, notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get the count of unread notifications for the authenticated user.
     * Used by the notification bell badge — polled every 30 seconds by Angular.
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Long> getUnreadCount() {
        String currentUserEmail = getCurrentUserEmail();

        long count = notificationService.getUnreadCount(currentUserEmail);

        logger.debug("Unread count retrieved | user={} | count={}", currentUserEmail, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark a single notification as read.
     * Validates ownership — only the recipient can mark their own notification.
     * Returns 404 if notification not found or doesn't belong to the user.
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        String currentUserEmail = getCurrentUserEmail();

        boolean success = notificationService.markAsRead(id, currentUserEmail);

        if (!success) {
            logger.warn("Mark as read failed | id={} | user={} | reason=not found or wrong owner",
                    id, currentUserEmail);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Notification not found or access denied");
        }

        logger.info("Notification marked as read | id={} | user={}", id, currentUserEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all unread notifications as read for the authenticated user.
     * Returns the number of notifications that were marked as read.
     */
    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Integer> markAllAsRead() {
        String currentUserEmail = getCurrentUserEmail();

        int updatedCount = notificationService.markAllAsRead(currentUserEmail);

        logger.info("All notifications marked as read | user={} | count={}", currentUserEmail, updatedCount);
        return ResponseEntity.ok(updatedCount);
    }
}