package com.buraqai.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_query_logs")
public class AIQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String questionText;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(nullable = false)
    private Boolean hasAnswer;

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(nullable = false)
    private Long responseTimeMs;

    @Column(nullable = false, length = 10)
    private String llmProvider;

    @Column(nullable = false)
    private String queriedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime queriedAt;

    @Column(nullable = false)
    private Boolean ticketCreated;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getHasAnswer() {
        return hasAnswer;
    }

    public void setHasAnswer(Boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getQueriedBy() {
        return queriedBy;
    }

    public void setQueriedBy(String queriedBy) {
        this.queriedBy = queriedBy;
    }

    public LocalDateTime getQueriedAt() {
        return queriedAt;
    }

    public Boolean getTicketCreated() {
        return ticketCreated;
    }

    public void setTicketCreated(Boolean ticketCreated) {
        this.ticketCreated = ticketCreated;
    }
}