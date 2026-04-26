package com.buraqai.backend.controller;

import com.buraqai.backend.dto.TicketResponseDTO;
import com.buraqai.backend.model.Ticket;
import com.buraqai.backend.repository.TicketRepository;
import com.buraqai.backend.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    public TicketController(TicketRepository ticketRepository, TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    /**
     * Get the email of the currently authenticated user from the JWT token.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // In JWT, getName() typically returns the subject (email)
    }

    /**
     * Get a specific ticket by ID.
     * - Employees can only view their own tickets (403 otherwise).
     * - Support agents, admins, and system admins can view any ticket.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<?> getTicketById(@PathVariable Long id) {
        String currentUserEmail = getCurrentUserEmail();

        // Find the ticket or return 404
        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            logger.warn("Ticket not found | id={} | requestedBy={}", id, currentUserEmail);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ticket not found");
        }

        // Check if current user is an employee (not agent/admin)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEmployeeOnly = auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_SUPPORT_AGENT")
                        || a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        // Employees can only view their own tickets
        if (isEmployeeOnly && !ticket.getCreatedBy().equals(currentUserEmail)) {
            logger.warn("Access denied to ticket | id={} | requestedBy={} | ticketOwner={}",
                    id, currentUserEmail, ticket.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only view your own tickets");
        }

        logger.info("Ticket retrieved | id={} | requestedBy={}", id, currentUserEmail);
        return ResponseEntity.ok(mapToDTO(ticket));
    }

    /**
     * Get all tickets created by the currently authenticated employee.
     * Sorted by createdAt descending (newest first).
     */
    @GetMapping("/my-tickets")
    @PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_SUPPORT_AGENT', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<List<TicketResponseDTO>> getMyTickets() {
        String currentUserEmail = getCurrentUserEmail();

        List<Ticket> tickets = ticketRepository.findByCreatedBy(currentUserEmail);

        // Sort newest first
        List<TicketResponseDTO> result = tickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        logger.info("My tickets retrieved | count={} | user={}", result.size(), currentUserEmail);
        return ResponseEntity.ok(result);
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