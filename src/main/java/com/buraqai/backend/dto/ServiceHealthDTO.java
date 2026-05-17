package com.buraqai.backend.dto;

import java.time.LocalDateTime;

public class ServiceHealthDTO {

    private String serviceName;
    private String status;        // "UP" or "DOWN"
    private Long responseTimeMs;  // response time in milliseconds, null if DOWN
    private String details;       // optional extra info (error message, model count, etc.)
    private LocalDateTime lastCheckedAt;

    // No-arg constructor (needed for Jackson deserialization)
    public ServiceHealthDTO() {
    }

    // All-args constructor for convenience
    public ServiceHealthDTO(String serviceName, String status, Long responseTimeMs,
                            String details, LocalDateTime lastCheckedAt) {
        this.serviceName = serviceName;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.details = details;
        this.lastCheckedAt = lastCheckedAt;
    }

    // Getters and setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}