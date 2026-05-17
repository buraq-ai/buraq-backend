package com.buraqai.backend.dto;

import java.util.List;
import java.util.Map;

public class AIStatsDTO {

    private Long totalQueries;
    private Long queriesWithAnswer;
    private Long queriesWithoutAnswer;
    private Double answerRate;
    private Long averageResponseTimeMs;
    private Double averageConfidenceScore;
    private List<DailyQueryCountDTO> queriesPerDay;
    private Map<String, Long> providerBreakdown;
    private Map<String, Long> languageBreakdown;
    private Double estimatedOpenAICost;

    // Default constructor (required for JSON deserialization)
    public AIStatsDTO() {}

    // Getters and Setters
    public Long getTotalQueries() {
        return totalQueries;
    }

    public void setTotalQueries(Long totalQueries) {
        this.totalQueries = totalQueries;
    }

    public Long getQueriesWithAnswer() {
        return queriesWithAnswer;
    }

    public void setQueriesWithAnswer(Long queriesWithAnswer) {
        this.queriesWithAnswer = queriesWithAnswer;
    }

    public Long getQueriesWithoutAnswer() {
        return queriesWithoutAnswer;
    }

    public void setQueriesWithoutAnswer(Long queriesWithoutAnswer) {
        this.queriesWithoutAnswer = queriesWithoutAnswer;
    }

    public Double getAnswerRate() {
        return answerRate;
    }

    public void setAnswerRate(Double answerRate) {
        this.answerRate = answerRate;
    }

    public Long getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(Long averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public Double getAverageConfidenceScore() {
        return averageConfidenceScore;
    }

    public void setAverageConfidenceScore(Double averageConfidenceScore) {
        this.averageConfidenceScore = averageConfidenceScore;
    }

    public List<DailyQueryCountDTO> getQueriesPerDay() {
        return queriesPerDay;
    }

    public void setQueriesPerDay(List<DailyQueryCountDTO> queriesPerDay) {
        this.queriesPerDay = queriesPerDay;
    }

    public Map<String, Long> getProviderBreakdown() {
        return providerBreakdown;
    }

    public void setProviderBreakdown(Map<String, Long> providerBreakdown) {
        this.providerBreakdown = providerBreakdown;
    }

    public Map<String, Long> getLanguageBreakdown() {
        return languageBreakdown;
    }

    public void setLanguageBreakdown(Map<String, Long> languageBreakdown) {
        this.languageBreakdown = languageBreakdown;
    }

    public Double getEstimatedOpenAICost() {
        return estimatedOpenAICost;
    }

    public void setEstimatedOpenAICost(Double estimatedOpenAICost) {
        this.estimatedOpenAICost = estimatedOpenAICost;
    }
}