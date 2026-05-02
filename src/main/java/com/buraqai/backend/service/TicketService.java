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
import com.buraqai.backend.dto.PaginatedResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.time.LocalDateTime;
import com.buraqai.backend.model.AuditLog;
import com.buraqai.backend.model.User;
import com.buraqai.backend.model.UserRole;
import com.buraqai.backend.repository.AuditLogRepository;
import com.buraqai.backend.repository.UserRepository;
import com.buraqai.backend.exception.TicketNotFoundException;
import com.buraqai.backend.exception.InvalidAssignmentException;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // Constructor injection — Spring wires all dependencies automatically
    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         AuditLogRepository auditLogRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
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
     * Retrieves paginated tickets assigned to a specific agent,
     * with optional status and date range filters.
     *
     * @param assignedTo The agent's email
     * @param status     Optional status filter (can be null)
     * @param fromDate   Optional start date filter (can be null)
     * @param toDate     Optional end date filter (can be null)
     * @param page       Page number (0-based)
     * @param size       Number of tickets per page
     * @return PaginatedResponseDTO containing tickets and pagination metadata
     */
    public PaginatedResponseDTO<TicketResponseDTO> getAssignedTickets(
            String assignedTo,
            TicketStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Ticket> ticketPage;

        if (status != null && fromDate != null && toDate != null) {
            // All three filters provided — this one doesn't exist in the repo,
            // we'll handle it by combining what we have
            ticketPage = ticketRepository.findByAssignedTo(assignedTo, pageRequest);
            // Filter in-memory for now (we'll optimize later if needed)
            ticketPage = ticketPage.map(ticket ->
                    ticket.getStatus() == status
                            && !ticket.getCreatedAt().isBefore(fromDate)
                            && !ticket.getCreatedAt().isAfter(toDate)
                            ? ticket : null
            );
        } else if (status != null) {
            // Only status filter
            ticketPage = ticketRepository.findByAssignedToAndStatus(assignedTo, status, pageRequest);
        } else if (fromDate != null && toDate != null) {
            // Only date range filter
            ticketPage = ticketRepository.findByAssignedToAndCreatedAtBetween(
                    assignedTo, fromDate, toDate, pageRequest
            );
        } else {
            // No filters — return all assigned tickets
            ticketPage = ticketRepository.findByAssignedTo(assignedTo, pageRequest);
        }

        // Convert Page<Ticket> to PaginatedResponseDTO<TicketResponseDTO>
        List<TicketResponseDTO> content = ticketPage.getContent()
                .stream()
                .map(this::mapToDTO)
                .toList();

        PaginatedResponseDTO<TicketResponseDTO> response = new PaginatedResponseDTO<>();
        response.setContent(content);
        response.setTotalElements(ticketPage.getTotalElements());
        response.setTotalPages(ticketPage.getTotalPages());
        response.setCurrentPage(ticketPage.getNumber());

        logger.info("Retrieved assigned tickets | agent={} | totalElements={} | page={}",
                assignedTo, ticketPage.getTotalElements(), page);

        return response;
    }

    /**
     * Assigns a ticket to a support agent.
     * Only System Admin can call this (enforced at controller level).
     *
     * @param ticketId    The ID of the ticket to assign
     * @param agentEmail  The email of the support agent
     * @param adminEmail  The email of the admin performing the assignment
     * @return Updated TicketResponseDTO
     * @throws TicketNotFoundException    if ticket doesn't exist
     * @throws InvalidAssignmentException if the email doesn't belong to an agent
     */
    @Transactional
    public TicketResponseDTO assignTicket(Long ticketId, String agentEmail, String adminEmail) {
        // 1. Find the ticket — throw if not found
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // 2. Validate the agent exists and has ROLE_SUPPORT_AGENT
        User agent = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new InvalidAssignmentException(
                        "The provided email does not belong to a support agent"));

        if (agent.getRole() != UserRole.ROLE_SUPPORT_AGENT) {
            throw new InvalidAssignmentException(
                    "The provided email does not belong to a support agent");
        }

        // 3. Assign the ticket
        ticket.setAssignedTo(agentEmail);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        Ticket updatedTicket = ticketRepository.save(ticket);

        // 4. Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("TICKET");
        auditLog.setEntityId(ticketId);
        auditLog.setAction("ASSIGN");
        auditLog.setPerformedBy(adminEmail);
        auditLog.setDetails("Ticket assigned to " + agentEmail);
        auditLogRepository.save(auditLog);

        logger.info("Ticket assigned | ticketId={} | assignedTo={} | assignedBy={}",
                ticketId, agentEmail, adminEmail);

        return mapToDTO(updatedTicket);
    }

    /**
     * Retrieves all tickets with optional status and date range filters.
     * Only accessible by System Admin.
     *
     * @param status   Optional status filter (can be null)
     * @param fromDate Optional start date filter (can be null)
     * @param toDate   Optional end date filter (can be null)
     * @param page     Page number (0-based)
     * @param size     Number of tickets per page
     * @return PaginatedResponseDTO containing tickets and pagination metadata
     */
    public PaginatedResponseDTO<TicketResponseDTO> getAllTickets(
            TicketStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Ticket> ticketPage;

        if (status != null && fromDate != null && toDate != null) {
            // All three filters — use date range + filter by status in-memory
            ticketPage = ticketRepository.findByCreatedAtBetween(fromDate, toDate, pageRequest);
            final TicketStatus finalStatus = status;
            ticketPage = ticketPage.map(ticket ->
                    ticket.getStatus() == finalStatus ? ticket : null
            );
        } else if (status != null) {
            // Only status filter
            ticketPage = ticketRepository.findByStatus(status, pageRequest);
        } else if (fromDate != null && toDate != null) {
            // Only date range filter
            ticketPage = ticketRepository.findByCreatedAtBetween(fromDate, toDate, pageRequest);
        } else {
            // No filters — return all tickets, newest first
            ticketPage = ticketRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        // Convert Page<Ticket> to PaginatedResponseDTO<TicketResponseDTO>
        List<TicketResponseDTO> content = ticketPage.getContent()
                .stream()
                .map(this::mapToDTO)
                .toList();

        PaginatedResponseDTO<TicketResponseDTO> response = new PaginatedResponseDTO<>();
        response.setContent(content);
        response.setTotalElements(ticketPage.getTotalElements());
        response.setTotalPages(ticketPage.getTotalPages());
        response.setCurrentPage(ticketPage.getNumber());

        logger.info("All tickets retrieved | totalElements={} | page={}",
                ticketPage.getTotalElements(), page);

        return response;
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