package com.buraqai.backend.service;

import com.buraqai.backend.dto.AIQueryRequestDTO;
import com.buraqai.backend.dto.AIQueryResponseDTO;
import com.buraqai.backend.model.AIQueryLog;
import com.buraqai.backend.repository.AIQueryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Service
public class AIQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AIQueryService.class);

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${llm.provider:OPENAI}")
    private String llmProvider;

    private RestTemplate restTemplate;
    private final AIQueryLogRepository aiQueryLogRepository;

    // Constructor injection for the repository
    public AIQueryService(AIQueryLogRepository aiQueryLogRepository) {
        this.aiQueryLogRepository = aiQueryLogRepository;
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(600000);

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Forwards the user's question to FastAPI RAG pipeline and returns the response.
     * Logs every query to the database for metrics tracking.
     *
     * @param request   Contains the question and language
     * @param userEmail The authenticated user's email
     * @return AIQueryResponseDTO with answer, sources, has_answer, and confidence_score
     * @throws RuntimeException if the AI service is unavailable or returns an error
     */
    public AIQueryResponseDTO queryAI(AIQueryRequestDTO request, String userEmail) {
        String url = aiServiceUrl + "/api/query";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIQueryRequestDTO> httpRequest = new HttpEntity<>(request, headers);

        // --- Step 1: Record start time ---
        long startTime = System.currentTimeMillis();

        AIQueryResponseDTO body = null;
        boolean success = true;

        try {
            logger.info("Forwarding query to FastAPI | language={} | question={}",
                    request.getLanguage(),
                    request.getQuestion().length() > 80
                            ? request.getQuestion().substring(0, 80) + "..."
                            : request.getQuestion()
            );

            ResponseEntity<AIQueryResponseDTO> response = restTemplate.postForEntity(
                    url,
                    httpRequest,
                    AIQueryResponseDTO.class
            );

            body = response.getBody();

            if (body == null) {
                logger.error("FastAPI returned an empty response body");
                throw new RuntimeException("AI service returned an empty response");
            }

            logger.info("Query response received | has_answer={} | confidence={} | sources={}",
                    body.getHasAnswer(),
                    body.getConfidenceScore(),
                    body.getSources() != null ? body.getSources().size() : 0
            );

            return body;

        } catch (RestClientException e) {
            success = false;
            logger.error("Failed to reach FastAPI for query. Error: {}", e.getMessage());
            throw new RuntimeException(
                    "AI service is currently unavailable. Please try again later.", e
            );
        } finally {
            // --- Step 2: Calculate response time and log ---
            long endTime = System.currentTimeMillis();
            long responseTimeMs = endTime - startTime;

            // Log even if the call failed (success = false)
            logQuery(request, body, userEmail, responseTimeMs, success);
        }
    }

    /**
     * Creates and saves an AIQueryLog entry for every AI query.
     * Uses a try-catch so logging failures never break the main flow.
     */
    private void logQuery(AIQueryRequestDTO request, AIQueryResponseDTO response,
                          String userEmail, long responseTimeMs, boolean success) {
        try {
            AIQueryLog log = new AIQueryLog();

            // Truncate question to first 500 characters
            String question = request.getQuestion();
            log.setQuestionText(question.length() > 500 ? question.substring(0, 500) : question);

            // Determine language: use detected language from response if available, otherwise request language
            String language = (response != null && response.getLanguageDetected() != null)
                    ? response.getLanguageDetected()
                    : request.getLanguage();
            log.setLanguage(language != null ? language : "en");

            log.setHasAnswer(response != null && response.getHasAnswer() != null
                    ? response.getHasAnswer() : false);

            log.setConfidenceScore(response != null && response.getConfidenceScore() != null
                    ? response.getConfidenceScore() : 0.0);

            log.setResponseTimeMs(responseTimeMs);

            log.setLlmProvider(llmProvider);

            log.setQueriedBy(userEmail);

            log.setTicketCreated(response != null && response.getTicketCreated() != null
                    ? response.getTicketCreated() : false);

            aiQueryLogRepository.save(log);

            logger.debug("AI query logged | provider={} | responseTimeMs={} | success={}",
                    llmProvider, responseTimeMs, success);

        } catch (Exception e) {
            // Never let logging failures affect the user's request
            logger.error("Failed to log AI query to database: {}", e.getMessage(), e);
        }
    }
}