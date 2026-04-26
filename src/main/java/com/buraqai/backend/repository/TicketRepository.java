package com.buraqai.backend.repository;

import com.buraqai.backend.model.Ticket;
import com.buraqai.backend.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCreatedBy(String email);
    List<Ticket> findByAssignedTo(String email);
    List<Ticket> findByStatus(TicketStatus status);
}