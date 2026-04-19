package com.buraqai.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AIQueryRequestDTO {

    @NotBlank(message = "Question must not be blank")
    private String question;

    private String language = "en";

    // Default constructor (required for JSON deserialization)
    public AIQueryRequestDTO() {}

    public AIQueryRequestDTO(String question, String language) {
        this.question = question;
        this.language = language;
    }

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}