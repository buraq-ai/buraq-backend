package com.buraqai.backend.service;

import com.buraqai.backend.dto.TicketResponseDTO;
import com.buraqai.backend.model.Ticket;
import com.buraqai.backend.model.TicketStatus;
import com.buraqai.backend.model.TicketSource;
import com.buraqai.backend.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;

    // Constructor injection — Spring wires the dependency automatically
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Creates a new ticket automatically from an AI fallback scenario.
     *
     * @param question       The full question text from the employee
     * @param createdByEmail The email of the employee who asked the question
     * @return TicketResponseDTO with the newly created ticket's details
     */
    @Transactional
    public TicketResponseDTO createTicket(String question, String createdByEmail) {
        Ticket ticket = new Ticket();

        // Title: first 100 characters of the question
        String title = question.length() > 100 ? question.substring(0, 100) : question;
        ticket.setTitle(title);

        // Description: the full question
        ticket.setDescription(question);

        // Status: always OPEN for new tickets
        ticket.setStatus(TicketStatus.OPEN);

        // Source: AI_FALLBACK since this is auto-created
        ticket.setSource(TicketSource.AI_FALLBACK);

        // Created by: the employee's email from JWT
        ticket.setCreatedBy(createdByEmail);

        // Assigned to: null — unassigned initially
        ticket.setAssignedTo(null);

        // Original question: store the full query for reference
        ticket.setOriginalQuestion(question);

        Ticket savedTicket = ticketRepository.save(ticket);

        logger.info("Ticket created automatically | id={} | title={} | createdBy={}",
                savedTicket.getId(),
                savedTicket.getTitle(),
                savedTicket.getCreatedBy()
        );

        return mapToDTO(savedTicket);
    }

    /**
     * Converts a Ticket entity to a TicketResponseDTO.
     */
    private TicketResponseDTO mapToDTO(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setStatus(ticket.getStatus());
        dto.setSource(ticket.getSource());
        dto.setCreatedBy(ticket.getCreatedBy());
        dto.setAssignedTo(ticket.getAssignedTo());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }
}