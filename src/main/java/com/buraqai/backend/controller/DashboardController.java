package com.buraqai.backend.controller;

import com.buraqai.backend.dto.AgentStatsDTO;
import com.buraqai.backend.dto.TicketStatsDTO;
import com.buraqai.backend.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get ticket statistics overview.
     * Supports optional fromDate and toDate query parameters for date range filtering.
     * Access: ROLE_SYSTEM_ADMIN only
     */
    @GetMapping("/ticket-stats")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<TicketStatsDTO> getTicketStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        TicketStatsDTO stats = dashboardService.getTicketStats(fromDate, toDate);

        logger.info("Ticket stats retrieved | fromDate={} | toDate={} | totalTickets={}",
                fromDate, toDate, stats.getTotalTickets());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get per-agent performance statistics.
     * Returns a list of agents with their ticket counts, resolution times, and resolution rates.
     * Access: ROLE_SYSTEM_ADMIN only
     */
    @GetMapping("/ticket-stats/by-agent")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<List<AgentStatsDTO>> getAgentStats() {
        List<AgentStatsDTO> agentStats = dashboardService.getAgentStats();

        logger.info("Agent stats retrieved | agentCount={}", agentStats.size());

        return ResponseEntity.ok(agentStats);
    }
}