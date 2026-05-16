package com.buraqai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsDTO {

    private Long totalTickets;
    private Long openTickets;
    private Long inProgressTickets;
    private Long closedTickets;
    private Double averageResolutionTimeHours;
    private Long slaBreachedTickets;
    private List<DailyTicketCountDTO> ticketsPerDay;

}