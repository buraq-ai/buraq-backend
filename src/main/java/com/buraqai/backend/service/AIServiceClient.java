package com.buraqai.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        // Configure timeouts to prevent hanging if AI service is down
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 seconds to establish connection
        factory.setReadTimeout(30000);     // 30 seconds to receive response

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Triggers the AI service to process and index a newly uploaded document.
     *
     * @param documentId The ID of the document in PostgreSQL
     * @param filePath   The full filesystem path to the uploaded file
     * @return true if the AI service accepted the request, false otherwise
     */
    public boolean triggerDocumentProcessing(Long documentId, String filePath) {
        String url = aiServiceUrl + "/internal/documents/process";

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("document_id", documentId);
        requestBody.put("file_path", filePath);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("Triggering AI processing for document ID: {} at path: {}", documentId, filePath);
            restTemplate.postForEntity(url, request, Void.class);
            logger.info("Successfully triggered AI processing for document ID: {}", documentId);
            return true;

        } catch (RestClientException e) {
            // Log the error but DO NOT fail the upload
            // The document stays in PENDING status and can be retried later
            logger.error("Failed to trigger AI processing for document ID: {}. " +
                            "AI service may be unavailable. Error: {}",
                    documentId, e.getMessage());
            return false;
        }
    }
}