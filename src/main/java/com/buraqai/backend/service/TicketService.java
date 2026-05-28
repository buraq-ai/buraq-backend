package com.buraqai.backend.service;

import com.buraqai.backend.dto.TicketResponseDTO;
import com.buraqai.backend.model.*;
import com.buraqai.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.buraqai.backend.dto.PaginatedResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.time.LocalDateTime;
import com.buraqai.backend.service.EmailService;
import com.buraqai.backend.exception.TicketNotFoundException;
import com.buraqai.backend.exception.InvalidAssignmentException;
import com.buraqai.backend.dto.ConversationMessageDTO;
import com.buraqai.backend.dto.TicketResponseRequestDTO;
import com.buraqai.backend.exception.UnauthorizedTicketAccessException;
import com.buraqai.backend.exception.TicketClosedException;
import com.buraqai.backend.exception.InvalidStatusTransitionException;
import com.buraqai.backend.dto.TicketStatusHistoryDTO;
import com.buraqai.backend.model.TicketStatusHistory;
import com.buraqai.backend.service.NotificationService;


@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TicketResponseRepository ticketResponseRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;



    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         AuditLogRepository auditLogRepository,
                         TicketResponseRepository ticketResponseRepository,
                         TicketStatusHistoryRepository ticketStatusHistoryRepository,
                         NotificationService notificationService,
                         EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.ticketResponseRepository = ticketResponseRepository;
        this.ticketStatusHistoryRepository = ticketStatusHistoryRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
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

        // Create in-app notification for the employee (non-critical — failure must not break ticket creation)
        try {
            // Create in-app notification
            notificationService.createNotification(
                    savedTicket.getCreatedBy(),
                    "Ticket Created — #" + savedTicket.getId(),
                    "Your support ticket has been created and is awaiting assignment. Your question: "
                            + savedTicket.getTitle(),
                    NotificationType.TICKET_CREATED,
                    savedTicket.getId()
            );
            logger.info("Notification created for ticket | ticketId={} | recipient={}",
                    savedTicket.getId(), savedTicket.getCreatedBy());

            // Send backup email notification
            emailService.sendTicketCreatedEmail(
                    savedTicket.getCreatedBy(),
                    savedTicket.getId(),
                    savedTicket.getTitle()
            );
        } catch (Exception e) {
            logger.error("Failed to create notification or send email for ticket | ticketId={} | error={}",
                    savedTicket.getId(), e.getMessage(), e);
            // Do not re-throw — notification/email failure must not fail ticket creation
        }

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

        // 4. Create TicketStatusHistory entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setPreviousStatus(TicketStatus.OPEN);
        history.setNewStatus(TicketStatus.IN_PROGRESS);
        history.setChangedBy(adminEmail);
        history.setChangedAt(LocalDateTime.now());
        history.setComment("Ticket assigned to " + agentEmail);
        ticketStatusHistoryRepository.save(history);

        // 5. Create audit log entry
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
     * Updates a ticket's status following the state machine rules.
     *
     * State machine:
     *   OPEN → IN_PROGRESS  (handled by assignTicket)
     *   IN_PROGRESS → CLOSED (agent or system admin)
     *   CLOSED → OPEN        (system admin only — reopen)
     *
     * @param ticketId      The ID of the ticket
     * @param newStatus     The desired new status
     * @param changerEmail  The email of the person changing the status
     * @param changerRole   The role of the person changing the status
     * @param comment       Optional comment for the status change
     * @return Updated TicketResponseDTO
     * @throws TicketNotFoundException           if ticket doesn't exist
     * @throws InvalidStatusTransitionException  if the transition is not allowed
     */
    @Transactional
    public TicketResponseDTO updateTicketStatus(Long ticketId,
                                                TicketStatus newStatus,
                                                String changerEmail,
                                                UserRole changerRole,
                                                String comment) {
        // 1. Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        TicketStatus previousStatus = ticket.getStatus();

        // 2. Authorize based on the type of transition
        if (newStatus == TicketStatus.CLOSED) {
            // Only assigned agent or system admin can close
            boolean isAssignedAgent = changerEmail.equals(ticket.getAssignedTo());
            boolean isSystemAdmin = changerRole == UserRole.ROLE_SYSTEM_ADMIN;

            if (!isAssignedAgent && !isSystemAdmin) {
                logger.warn("Unauthorized close attempt | ticketId={} | changer={} | role={}",
                        ticketId, changerEmail, changerRole);
                throw new UnauthorizedTicketAccessException(
                        "Only the assigned agent or a system admin can close this ticket");
            }
        } else if (newStatus == TicketStatus.OPEN) {
            // Only system admin can reopen
            if (changerRole != UserRole.ROLE_SYSTEM_ADMIN) {
                logger.warn("Unauthorized reopen attempt | ticketId={} | changer={} | role={}",
                        ticketId, changerEmail, changerRole);
                throw new UnauthorizedTicketAccessException(
                        "Only a system admin can reopen a ticket");
            }
        }

        // 3. Validate the transition is allowed by the state machine
        boolean isValidTransition = false;

        if (previousStatus == TicketStatus.IN_PROGRESS && newStatus == TicketStatus.CLOSED) {
            isValidTransition = true;
        } else if (previousStatus == TicketStatus.CLOSED && newStatus == TicketStatus.OPEN) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new InvalidStatusTransitionException(previousStatus, newStatus);
        }

        // 4. Update the ticket
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 5. Create TicketStatusHistory entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changerEmail);
        history.setChangedAt(LocalDateTime.now());
        history.setComment(comment);
        ticketStatusHistoryRepository.save(history);

        // 6. Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("TICKET");
        auditLog.setEntityId(ticketId);
        auditLog.setAction("STATUS_CHANGE");
        auditLog.setPerformedBy(changerEmail);
        auditLog.setDetails("Status changed from " + previousStatus + " to " + newStatus
                + " by " + changerEmail);
        auditLogRepository.save(auditLog);

        logger.info("Ticket status updated | ticketId={} | {} → {} | by={}",
                ticketId, previousStatus, newStatus, changerEmail);

        return mapToDTO(updatedTicket);
    }

    /**
     * Retrieves the status change history for a ticket.
     * Only the ticket owner, assigned agent, or system admin can view.
     *
     * @param ticketId       The ID of the ticket
     * @param requesterEmail The email of the person requesting the history
     * @param requesterRole  The role of the requester
     * @return List of TicketStatusHistoryDTO sorted oldest first
     * @throws TicketNotFoundException         if ticket doesn't exist
     * @throws UnauthorizedTicketAccessException if requester is not authorized
     */
    public List<TicketStatusHistoryDTO> getTicketHistory(Long ticketId,
                                                         String requesterEmail,
                                                         UserRole requesterRole) {
        // 1. Find the ticket — throw if not found
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // 2. Authorize: only ticket owner, assigned agent, or System Admin can view
        boolean isTicketOwner = requesterEmail.equals(ticket.getCreatedBy());
        boolean isAssignedAgent = requesterEmail.equals(ticket.getAssignedTo());
        boolean isSystemAdmin = requesterRole == UserRole.ROLE_SYSTEM_ADMIN;

        if (!isTicketOwner && !isAssignedAgent && !isSystemAdmin) {
            throw new UnauthorizedTicketAccessException(
                    "You are not authorized to view this ticket's history");
        }

        // 3. Fetch history sorted oldest first
        List<TicketStatusHistory> history = ticketStatusHistoryRepository
                .findByTicketIdOrderByChangedAtAsc(ticketId);

        logger.info("Ticket history retrieved | ticketId={} | requester={} | entryCount={}",
                ticketId, requesterEmail, history.size());

        // 4. Convert to DTOs and return
        return history.stream()
                .map(this::mapHistoryToDTO)
                .toList();
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

    /**
     * Adds a response to a ticket's conversation thread.
     *
     * @param ticketId      The ID of the ticket
     * @param requestDTO    Contains the response text
     * @param responderEmail The email of the person responding
     * @param responderRole  The role of the person responding
     * @return ConversationMessageDTO with the saved response
     * @throws TicketNotFoundException         if ticket doesn't exist
     * @throws UnauthorizedTicketAccessException if responder is not authorized
     * @throws TicketClosedException           if ticket is CLOSED
     */
    @Transactional
    public ConversationMessageDTO addResponse(Long ticketId,
                                              TicketResponseRequestDTO requestDTO,
                                              String responderEmail,
                                              UserRole responderRole) {
        // 1. Find the ticket — throw if not found
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // 2. Validate the ticket is not CLOSED
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketClosedException("Cannot add response to a closed ticket");
        }

        // 3. Authorize the responder and determine if this is an agent response
        boolean isAgentResponse;

        boolean isTicketOwner = responderEmail.equals(ticket.getCreatedBy());
        boolean isAssignedAgent = responderEmail.equals(ticket.getAssignedTo());
        boolean isSystemAdmin = responderRole == UserRole.ROLE_SYSTEM_ADMIN;

        if (isTicketOwner) {
            // Employee responding to their own ticket
            isAgentResponse = false;
        } else if (isAssignedAgent || isSystemAdmin) {
            // Assigned agent or System Admin responding
            isAgentResponse = true;
        } else {
            throw new UnauthorizedTicketAccessException(
                    "You are not authorized to respond to this ticket");
        }

        // 4. Create and save the TicketResponse entity
        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setResponseText(requestDTO.getResponseText());
        response.setRespondedBy(responderEmail);
        response.setIsAgentResponse(isAgentResponse);

        TicketResponse savedResponse = ticketResponseRepository.save(response);

        // 5. Update the ticket's updatedAt timestamp
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // 6. Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("TICKET");
        auditLog.setEntityId(ticketId);
        auditLog.setAction("RESPONSE_ADDED");
        auditLog.setPerformedBy(responderEmail);
        auditLog.setDetails("Response added by " + responderEmail);
        auditLogRepository.save(auditLog);

        logger.info("Response added to ticket | ticketId={} | responder={} | isAgentResponse={}",
                ticketId, responderEmail, isAgentResponse);

        // 7. Convert to DTO and return
        return mapResponseToDTO(savedResponse);
    }

    /**
     * Retrieves the conversation history for a ticket.
     *
     * @param ticketId       The ID of the ticket
     * @param requesterEmail The email of the person requesting the conversation
     * @param requesterRole  The role of the requester
     * @return List of ConversationMessageDTO sorted oldest first
     * @throws TicketNotFoundException         if ticket doesn't exist
     * @throws UnauthorizedTicketAccessException if requester is not authorized to view
     */
    public List<ConversationMessageDTO> getTicketResponses(Long ticketId,
                                                           String requesterEmail,
                                                           UserRole requesterRole) {
        // 1. Find the ticket — throw if not found
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        // 2. Authorize: only ticket owner, assigned agent, or System Admin can view
        boolean isTicketOwner = requesterEmail.equals(ticket.getCreatedBy());
        boolean isAssignedAgent = requesterEmail.equals(ticket.getAssignedTo());
        boolean isSystemAdmin = requesterRole == UserRole.ROLE_SYSTEM_ADMIN;

        if (!isTicketOwner && !isAssignedAgent && !isSystemAdmin) {
            throw new UnauthorizedTicketAccessException(
                    "You are not authorized to view this conversation");
        }

        // 3. Fetch responses sorted oldest first
        List<TicketResponse> responses = ticketResponseRepository
                .findByTicketIdOrderByRespondedAtAsc(ticketId);

        logger.info("Conversation retrieved | ticketId={} | requester={} | messageCount={}",
                ticketId, requesterEmail, responses.size());

        // 4. Convert to DTOs and return
        return responses.stream()
                .map(this::mapResponseToDTO)
                .toList();
    }


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

    /**
     * Converts a TicketResponse entity to a ConversationMessageDTO.
     */
    private ConversationMessageDTO mapResponseToDTO(TicketResponse response) {
        ConversationMessageDTO dto = new ConversationMessageDTO();
        dto.setId(response.getId());
        dto.setResponseText(response.getResponseText());
        dto.setRespondedBy(response.getRespondedBy());
        dto.setRespondedAt(response.getRespondedAt());
        dto.setIsAgentResponse(response.getIsAgentResponse());
        return dto;
    }

    /**
     * Converts a TicketStatusHistory entity to a TicketStatusHistoryDTO.
     */
    private TicketStatusHistoryDTO mapHistoryToDTO(TicketStatusHistory history) {
        TicketStatusHistoryDTO dto = new TicketStatusHistoryDTO();
        dto.setId(history.getId());
        dto.setPreviousStatus(history.getPreviousStatus());
        dto.setNewStatus(history.getNewStatus());
        dto.setChangedBy(history.getChangedBy());
        dto.setChangedAt(history.getChangedAt());
        dto.setComment(history.getComment());
        return dto;
    }
}