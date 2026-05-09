package com.buraqai.backend.repository;

import com.buraqai.backend.model.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {

    List<TicketResponse> findByTicketIdOrderByRespondedAtAsc(Long ticketId);
}