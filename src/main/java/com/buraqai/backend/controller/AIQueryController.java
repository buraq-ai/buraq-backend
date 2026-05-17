package com.buraqai.backend.controller;

import com.buraqai.backend.dto.AIQueryRequestDTO;
import com.buraqai.backend.dto.AIQueryResponseDTO;
import com.buraqai.backend.dto.TicketResponseDTO;
import com.buraqai.backend.service.AIQueryService;
import com.buraqai.backend.service.TicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
public class AIQueryController {

    private static final Logger logger = LoggerFactory.getLogger(AIQueryController.class);

    private final AIQueryService aiQueryService;
    private final TicketService ticketService;

    public AIQueryController(AIQueryService aiQueryService, TicketService ticketService) {
        this.aiQueryService = aiQueryService;
        this.ticketService = ticketService;
    }

    /**
     * Bridge endpoint — receives question from Angular and forwards to FastAPI RAG pipeline.
     * If the AI cannot answer and should_create_ticket is true,
     * automatically creates a support ticket for the employee.
     *
     * @param request Contains question and language
     * @return AIQueryResponseDTO with answer details and optionally ticketId
     */
    @PostMapping("/query")
    public ResponseEntity<AIQueryResponseDTO> query(@Valid @RequestBody AIQueryRequestDTO request) {

        // Extract the asking user's email from the JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        logger.info("Query request from user: {} | language: {} | question: {}",
                userEmail,
                request.getLanguage(),
                request.getQuestion().length() > 80
                        ? request.getQuestion().substring(0, 80) + "..."
                        : request.getQuestion()
        );

        // Step 1: Forward the question to FastAPI
        AIQueryResponseDTO response = aiQueryService.queryAI(request, userEmail);

        // Step 2: If AI cannot answer and ticket creation is requested, create a ticket
        if (response.getShouldCreateTicket() != null && response.getShouldCreateTicket()
                && response.getHasAnswer() != null && !response.getHasAnswer()) {

            logger.info("AI fallback triggered — creating ticket for user: {}", userEmail);

            try {
                TicketResponseDTO ticket = ticketService.createTicket(
                        request.getQuestion(),
                        userEmail
                );

                // Populate the response with ticket details
                response.setTicketCreated(true);
                response.setTicketId(ticket.getId());

                logger.info("Ticket created and linked to response | ticketId={}", ticket.getId());

            } catch (Exception e) {
                // Log the error but do NOT break the AI query flow
                logger.error("Ticket creation failed for user: {} | error: {}", userEmail, e.getMessage(), e);
                response.setTicketCreated(false);
                response.setTicketId(null);
            }
        } else {
            // AI answered — no ticket needed
            response.setTicketCreated(false);
            response.setTicketId(null);
        }

        return ResponseEntity.ok(response); // HTTP 200
    }
}