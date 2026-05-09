package com.buraqai.backend.dto;

import java.time.LocalDateTime;

public class ConversationMessageDTO {

    private Long id;
    private String responseText;
    private String respondedBy;
    private LocalDateTime respondedAt;
    private Boolean isAgentResponse;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(String respondedBy) {
        this.respondedBy = respondedBy;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Boolean getIsAgentResponse() {
        return isAgentResponse;
    }

    public void setIsAgentResponse(Boolean isAgentResponse) {
        this.isAgentResponse = isAgentResponse;
    }
}