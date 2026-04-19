package com.buraqai.backend.service;

import com.buraqai.backend.dto.AIQueryRequestDTO;
import com.buraqai.backend.dto.AIQueryResponseDTO;
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

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        // Longer read timeout than document processing — LLM can take time to respond
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);   // 10 seconds to connect
        factory.setReadTimeout(600000);     // 3 minutes for LLM to respond

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Forwards the user's question to FastAPI RAG pipeline and returns the response.
     *
     * @param request Contains the question and language
     * @return AIQueryResponseDTO with answer, sources, has_answer, and confidence_score
     * @throws RuntimeException if the AI service is unavailable or returns an error
     */
    public AIQueryResponseDTO queryAI(AIQueryRequestDTO request) {
        String url = aiServiceUrl + "/api/query";

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIQueryRequestDTO> httpRequest = new HttpEntity<>(request, headers);

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

            AIQueryResponseDTO body = response.getBody();

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
            logger.error("Failed to reach FastAPI for query. Error: {}", e.getMessage());
            throw new RuntimeException(
                    "AI service is currently unavailable. Please try again later.", e
            );
        }
    }
}