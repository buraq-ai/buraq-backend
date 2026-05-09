package com.buraqai.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_responses")
public class TicketResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 2000)
    private String responseText;

    @Column(nullable = false)
    private String respondedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private Boolean isAgentResponse;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
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

    public Boolean getIsAgentResponse() {
        return isAgentResponse;
    }

    public void setIsAgentResponse(Boolean isAgentResponse) {
        this.isAgentResponse = isAgentResponse;
    }
}