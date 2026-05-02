package com.buraqai.backend.repository;

import com.buraqai.backend.model.Ticket;
import com.buraqai.backend.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // --- Existing methods (unchanged) ---
    List<Ticket> findByCreatedBy(String email);
    List<Ticket> findByAssignedTo(String email);
    List<Ticket> findByStatus(TicketStatus status);

    // --- New paginated methods for Agent Dashboard ---
    Page<Ticket> findByAssignedTo(String assignedTo, Pageable pageable);

    Page<Ticket> findByAssignedToAndStatus(
            String assignedTo,
            TicketStatus status,
            Pageable pageable
    );

    Page<Ticket> findByAssignedToAndCreatedAtBetween(
            String assignedTo,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );
    // --- New paginated methods for Admin Dashboard ---
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByCreatedAtBetween(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

}