package com.buraqai.backend.repository;

import com.buraqai.backend.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find unread notifications for a user, newest first
    List<Notification> findByRecipientEmailAndIsReadFalseOrderByCreatedAtDesc(String recipientEmail);

    // Find all notifications for a user with pagination, newest first
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    // Count unread notifications for a user
    long countByRecipientEmailAndIsReadFalse(String recipientEmail);

    // Mark a single notification as read (validates ownership via recipientEmail)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipientEmail = :recipientEmail")
    int markAsRead(@Param("id") Long id, @Param("recipientEmail") String recipientEmail);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientEmail = :recipientEmail AND n.isRead = false")
    int markAllAsRead(@Param("recipientEmail") String recipientEmail);
}