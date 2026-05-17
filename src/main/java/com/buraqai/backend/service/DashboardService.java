package com.buraqai.backend.service;

import com.buraqai.backend.dto.AgentStatsDTO;
import com.buraqai.backend.dto.DailyTicketCountDTO;
import com.buraqai.backend.dto.TicketStatsDTO;
import com.buraqai.backend.model.Ticket;
import com.buraqai.backend.model.TicketStatus;
import com.buraqai.backend.model.TicketStatusHistory;
import com.buraqai.backend.repository.AIQueryLogRepository;
import com.buraqai.backend.repository.TicketRepository;
import com.buraqai.backend.repository.TicketStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.buraqai.backend.dto.AIStatsDTO;
import com.buraqai.backend.dto.DailyQueryCountDTO;
import com.buraqai.backend.model.AIQueryLog;
import com.buraqai.backend.dto.ServiceHealthDTO;
import com.buraqai.backend.dto.SystemHealthDTO;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final AIQueryLogRepository aiQueryLogRepository;

    private static final int SLA_HOURS = 48;
    private static final int DEFAULT_DAYS = 30;

    public TicketStatsDTO getTicketStats(LocalDate fromDate, LocalDate toDate) {
        // Default to last 30 days if no dates provided
        LocalDate endDate = (toDate != null) ? toDate : LocalDate.now();
        LocalDate startDate = (fromDate != null) ? fromDate : endDate.minusDays(DEFAULT_DAYS);

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toDateTime = endDate.plusDays(1).atStartOfDay(); // inclusive end

        List<Ticket> ticketsInRange = ticketRepository.findByCreatedAtBetween(
                fromDateTime, toDateTime, null
        ).getContent();

        // If pagination gives issues, we can load all. For stats we need all tickets.
        List<Ticket> allTicketsInRange = ticketRepository.findAll().stream()
                .filter(t -> !t.getCreatedAt().isBefore(fromDateTime) && t.getCreatedAt().isBefore(toDateTime))
                .collect(Collectors.toList());

        long totalTickets = allTicketsInRange.size();
        long openTickets = countByStatus(allTicketsInRange, TicketStatus.OPEN);
        long inProgressTickets = countByStatus(allTicketsInRange, TicketStatus.IN_PROGRESS);
        long closedTickets = countByStatus(allTicketsInRange, TicketStatus.CLOSED);

        // SLA breached: OPEN tickets older than 48 hours
        LocalDateTime slaThreshold = LocalDateTime.now().minusHours(SLA_HOURS);
        long slaBreached = allTicketsInRange.stream()
                .filter(t -> t.getStatus() == TicketStatus.OPEN && t.getCreatedAt().isBefore(slaThreshold))
                .count();

        // Average resolution time (for closed tickets only)
        double avgResolutionHours = calculateAverageResolutionTime(allTicketsInRange);

        // Tickets per day (last 30 days from startDate)
        List<DailyTicketCountDTO> ticketsPerDay = calculateTicketsPerDay(allTicketsInRange, startDate, endDate);

        TicketStatsDTO stats = new TicketStatsDTO();
        stats.setTotalTickets(totalTickets);
        stats.setOpenTickets(openTickets);
        stats.setInProgressTickets(inProgressTickets);
        stats.setClosedTickets(closedTickets);
        stats.setAverageResolutionTimeHours(avgResolutionHours);
        stats.setSlaBreachedTickets(slaBreached);
        stats.setTicketsPerDay(ticketsPerDay);

        return stats;
    }

    public List<AgentStatsDTO> getAgentStats() {
        List<Ticket> allTickets = ticketRepository.findAll();

        // Group tickets by assignedTo (skip unassigned)
        Map<String, List<Ticket>> ticketsByAgent = allTickets.stream()
                .filter(t -> t.getAssignedTo() != null && !t.getAssignedTo().isBlank())
                .collect(Collectors.groupingBy(Ticket::getAssignedTo));

        List<AgentStatsDTO> agentStats = new ArrayList<>();

        for (Map.Entry<String, List<Ticket>> entry : ticketsByAgent.entrySet()) {
            String agentEmail = entry.getKey();
            List<Ticket> agentTickets = entry.getValue();

            long totalAssigned = agentTickets.size();
            long totalClosed = agentTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CLOSED)
                    .count();

            double avgResolution = calculateAverageResolutionTime(agentTickets);

            double resolutionRate = (totalAssigned > 0)
                    ? (totalClosed * 100.0 / totalAssigned)
                    : 0.0;

            AgentStatsDTO dto = new AgentStatsDTO();
            dto.setAgentEmail(agentEmail);
            dto.setTotalAssigned(totalAssigned);
            dto.setTotalClosed(totalClosed);
            dto.setAverageResolutionTimeHours(avgResolution);
            dto.setResolutionRate(Math.round(resolutionRate * 100.0) / 100.0); // round to 2 decimals

            agentStats.add(dto);
        }

        // Sort by resolutionRate descending
        agentStats.sort((a, b) -> Double.compare(b.getResolutionRate(), a.getResolutionRate()));

        return agentStats;
    }


    /**
     * Aggregates AI query metrics for the System Admin dashboard.
     * Includes totals, rates, response times, provider/language breakdowns,
     * daily query counts, and estimated OpenAI cost.
     *
     * @return AIStatsDTO with all aggregated metrics
     */
    public AIStatsDTO getAIStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        // --- Total counts ---
        long totalQueries = aiQueryLogRepository.count();
        long queriesWithAnswer = aiQueryLogRepository.countByHasAnswer(true);
        long queriesWithoutAnswer = aiQueryLogRepository.countByHasAnswer(false);

        // --- Answer rate (percentage) ---
        double answerRate = (totalQueries > 0)
                ? Math.round((queriesWithAnswer * 100.0 / totalQueries) * 10.0) / 10.0
                : 0.0;

        // --- Average response time ---
        Long avgResponseTime = aiQueryLogRepository.findAverageResponseTimeMs();
        long averageResponseTimeMs = (avgResponseTime != null) ? avgResponseTime : 0L;

        // --- Average confidence score ---
        Double avgConfidence = aiQueryLogRepository.findAverageConfidenceScore();
        double averageConfidenceScore = (avgConfidence != null)
                ? Math.round(avgConfidence * 100.0) / 100.0
                : 0.0;

        // --- Provider breakdown ---
        Map<String, Long> providerBreakdown = new HashMap<>();
        List<Object[]> providerCounts = aiQueryLogRepository.countByProvider();
        for (Object[] row : providerCounts) {
            String provider = (String) row[0];
            Long count = (Long) row[1];
            providerBreakdown.put(provider, count);
        }

        // --- Language breakdown ---
        Map<String, Long> languageBreakdown = new HashMap<>();
        List<Object[]> languageCounts = aiQueryLogRepository.countByLanguage();
        for (Object[] row : languageCounts) {
            String language = (String) row[0];
            Long count = (Long) row[1];
            languageBreakdown.put(language, count);
        }

        // --- Queries per day (last 30 days) ---
        List<DailyQueryCountDTO> queriesPerDay = new ArrayList<>();
        List<Object[]> dailyCounts = aiQueryLogRepository.countQueriesPerDay(thirtyDaysAgo, now);

        // Build a map of date -> count for quick lookup
        Map<LocalDate, Long> countByDate = new HashMap<>();
        for (Object[] row : dailyCounts) {
            // The date comes back as java.sql.Date from function('date', ...)
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            countByDate.put(date, count);
        }

        // Fill in all 30 days, even those with 0 queries (for a continuous line chart)
        LocalDate current = thirtyDaysAgo.toLocalDate();
        LocalDate end = now.toLocalDate();
        while (!current.isAfter(end)) {
            DailyQueryCountDTO dto = new DailyQueryCountDTO();
            dto.setDate(current);
            dto.setCount(countByDate.getOrDefault(current, 0L));
            queriesPerDay.add(dto);
            current = current.plusDays(1);
        }

        // --- Estimated OpenAI cost ($0.002 per query) ---
        long openaiCount = aiQueryLogRepository.countByLlmProvider("OPENAI");
        double estimatedOpenAICost = Math.round(openaiCount * 0.002 * 100.0) / 100.0;

        // --- Build and return DTO ---
        AIStatsDTO stats = new AIStatsDTO();
        stats.setTotalQueries(totalQueries);
        stats.setQueriesWithAnswer(queriesWithAnswer);
        stats.setQueriesWithoutAnswer(queriesWithoutAnswer);
        stats.setAnswerRate(answerRate);
        stats.setAverageResponseTimeMs(averageResponseTimeMs);
        stats.setAverageConfidenceScore(averageConfidenceScore);
        stats.setQueriesPerDay(queriesPerDay);
        stats.setProviderBreakdown(providerBreakdown);
        stats.setLanguageBreakdown(languageBreakdown);
        stats.setEstimatedOpenAICost(estimatedOpenAICost);

        return stats;
    }

    /**
     * Checks the health of all system components:
     * Spring Boot, PostgreSQL, FastAPI, ChromaDB, and Ollama.
     * Measures response time for each and sets overallStatus
     * to DOWN if any single service is unreachable.
     *
     * @return SystemHealthDTO with per-service status and timings
     */
    public SystemHealthDTO getSystemHealth() {
        LocalDateTime checkedAt = LocalDateTime.now();
        List<ServiceHealthDTO> services = new ArrayList<>();
        boolean allUp = true;

        // 1. Spring Boot — always UP if this method is executing
        long startBoot = System.currentTimeMillis();
        long bootResponseTime = System.currentTimeMillis() - startBoot;
        services.add(new ServiceHealthDTO(
                "Spring Boot",
                "UP",
                bootResponseTime,
                "Application is running",
                checkedAt
        ));

        // 2. PostgreSQL — use repository count query
        long startDb = System.currentTimeMillis();
        try {
            ticketRepository.count();
            long dbResponseTime = System.currentTimeMillis() - startDb;
            services.add(new ServiceHealthDTO(
                    "PostgreSQL",
                    "UP",
                    dbResponseTime,
                    "Database connection successful",
                    checkedAt
            ));
        } catch (Exception e) {
            allUp = false;
            services.add(new ServiceHealthDTO(
                    "PostgreSQL",
                    "DOWN",
                    null,
                    e.getMessage(),
                    checkedAt
            ));
        }

        // 3. FastAPI — GET http://localhost:8000/health
        services.add(checkExternalService(
                "FastAPI",
                "http://localhost:8001/health",
                checkedAt
        ));
        if ("DOWN".equals(services.get(services.size() - 1).getStatus())) {
            allUp = false;
        }

        // 4. ChromaDB — heartbeat endpoint
        services.add(checkExternalService(
                "ChromaDB",
                "http://localhost:8000/api/v2/heartbeat",
                checkedAt
        ));
        if ("DOWN".equals(services.get(services.size() - 1).getStatus())) {
            allUp = false;
        }

        // 5. Ollama — GET http://localhost:11434/api/tags
        services.add(checkOllamaService(checkedAt));
        if ("DOWN".equals(services.get(services.size() - 1).getStatus())) {
            allUp = false;
        }

        return new SystemHealthDTO(
                allUp ? "UP" : "DOWN",
                services,
                checkedAt
        );
    }
    // --- Private helper methods ---

    /**
     * Generic health check for an external HTTP service.
     * Calls the given URL and reports UP if it receives any successful response.
     */
    private ServiceHealthDTO checkExternalService(String serviceName, String url, LocalDateTime checkedAt) {
        RestTemplate restTemplate = createRestTemplateWithTimeout();
        long start = System.currentTimeMillis();
        try {
            restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            long responseTime = System.currentTimeMillis() - start;
            return new ServiceHealthDTO(serviceName, "UP", responseTime,
                    "Service is reachable", checkedAt);
        } catch (Exception e) {
            return new ServiceHealthDTO(serviceName, "DOWN", null,
                    e.getMessage(), checkedAt);
        }
    }

    /**
     * Health check specifically for Ollama.
     * Uses /api/tags to verify the service and capture available models.
     */
    private ServiceHealthDTO checkOllamaService(LocalDateTime checkedAt) {
        RestTemplate restTemplate = createRestTemplateWithTimeout();
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:11434/api/tags",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );
            long responseTime = System.currentTimeMillis() - start;
            Map<String, Object> body = response.getBody();
            int modelCount = 0;
            if (body != null && body.get("models") instanceof List) {
                modelCount = ((List<?>) body.get("models")).size();
            }
            return new ServiceHealthDTO("Ollama", "UP", responseTime,
                    modelCount + " model(s) available", checkedAt);
        } catch (Exception e) {
            return new ServiceHealthDTO("Ollama", "DOWN", null,
                    e.getMessage(), checkedAt);
        }
    }

    /**
     * Creates a RestTemplate with 3-second connect and read timeouts.
     */
    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }


    private long countByStatus(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    private double calculateAverageResolutionTime(List<Ticket> tickets) {
        List<Ticket> closedTickets = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.CLOSED)
                .collect(Collectors.toList());

        if (closedTickets.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0.0;
        int count = 0;

        for (Ticket ticket : closedTickets) {
            List<TicketStatusHistory> history = statusHistoryRepository
                    .findByTicketIdOrderByChangedAtAsc(ticket.getId());

            // Find the first CLOSED status entry
            Optional<TicketStatusHistory> firstClosed = history.stream()
                    .filter(h -> h.getNewStatus() == TicketStatus.CLOSED)
                    .findFirst();

            if (firstClosed.isPresent()) {
                long hours = ChronoUnit.HOURS.between(ticket.getCreatedAt(), firstClosed.get().getChangedAt());
                totalHours += hours;
                count++;
            }
        }

        return (count > 0) ? Math.round((totalHours / count) * 100.0) / 100.0 : 0.0;
    }

    private List<DailyTicketCountDTO> calculateTicketsPerDay(List<Ticket> tickets, LocalDate startDate, LocalDate endDate) {
        // Group tickets by creation date
        Map<LocalDate, Long> countByDate = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Fill in all days in the range, even those with 0 tickets
        List<DailyTicketCountDTO> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DailyTicketCountDTO dto = new DailyTicketCountDTO();
            dto.setDate(current);
            dto.setCount(countByDate.getOrDefault(current, 0L));
            result.add(dto);
            current = current.plusDays(1);
        }

        return result;
    }
}