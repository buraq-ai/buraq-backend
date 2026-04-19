package com.buraqai.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AIQueryResponseDTO {

    private String question;
    private String answer;
    private List<SourceChunkDTO> sources;

    @JsonProperty("has_answer")
    private Boolean hasAnswer;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    // Default constructor (required for JSON deserialization)
    public AIQueryResponseDTO() {}

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<SourceChunkDTO> getSources() { return sources; }
    public void setSources(List<SourceChunkDTO> sources) { this.sources = sources; }

    public Boolean getHasAnswer() { return hasAnswer; }
    public void setHasAnswer(Boolean hasAnswer) { this.hasAnswer = hasAnswer; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
}