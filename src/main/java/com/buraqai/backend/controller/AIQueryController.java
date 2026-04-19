package com.buraqai.backend.controller;

import com.buraqai.backend.dto.AIQueryRequestDTO;
import com.buraqai.backend.dto.AIQueryResponseDTO;
import com.buraqai.backend.service.AIQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
public class AIQueryController {

    private final AIQueryService aiQueryService;

    public AIQueryController(AIQueryService aiQueryService) {
        this.aiQueryService = aiQueryService;
    }

    /**
     * Bridge endpoint — receives question from Angular and forwards to FastAPI RAG pipeline.
     * Requires a valid JWT token (any role).
     *
     * @param request Contains question and language
     * @return AIQueryResponseDTO with answer, sources, has_answer, confidence_score
     */
    @PostMapping("/query")
    public ResponseEntity<AIQueryResponseDTO> query(@Valid @RequestBody AIQueryRequestDTO request) {

        // Extract the asking user's email from the JWT token for logging
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        org.slf4j.LoggerFactory.getLogger(AIQueryController.class)
                .info("Query request from user: {} | language: {} | question: {}",
                        userEmail,
                        request.getLanguage(),
                        request.getQuestion().length() > 80
                                ? request.getQuestion().substring(0, 80) + "..."
                                : request.getQuestion()
                );

        AIQueryResponseDTO response = aiQueryService.queryAI(request);

        return ResponseEntity.ok(response); // HTTP 200
    }
}