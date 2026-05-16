package com.buraqai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatsDTO {

    private String agentEmail;
    private Long totalAssigned;
    private Long totalClosed;
    private Double averageResolutionTimeHours;
    private Double resolutionRate;

}