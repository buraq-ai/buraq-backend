package com.buraqai.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SystemHealthDTO {

    private String overallStatus;         // "UP" if all services UP, "DOWN" if any DOWN
    private List<ServiceHealthDTO> services;
    private LocalDateTime checkedAt;

    public SystemHealthDTO() {
    }

    public SystemHealthDTO(String overallStatus, List<ServiceHealthDTO> services, LocalDateTime checkedAt) {
        this.overallStatus = overallStatus;
        this.services = services;
        this.checkedAt = checkedAt;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<ServiceHealthDTO> getServices() {
        return services;
    }

    public void setServices(List<ServiceHealthDTO> services) {
        this.services = services;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}