package com.buraqai.backend.repository;

import com.buraqai.backend.model.AIQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AIQueryLogRepository extends JpaRepository<AIQueryLog, Long> {

    // --- Count queries ---

    long count();

    long countByHasAnswer(Boolean hasAnswer);

    // --- Average metrics ---

    @Query("SELECT AVG(q.responseTimeMs) FROM AIQueryLog q")
    Long findAverageResponseTimeMs();

    @Query("SELECT AVG(q.confidenceScore) FROM AIQueryLog q")
    Double findAverageConfidenceScore();

    // --- Provider breakdown: returns count per provider ---

    @Query("SELECT q.llmProvider, COUNT(q) FROM AIQueryLog q GROUP BY q.llmProvider")
    List<Object[]> countByProvider();

    // --- Language breakdown: returns count per language ---

    @Query("SELECT q.language, COUNT(q) FROM AIQueryLog q GROUP BY q.language")
    List<Object[]> countByLanguage();

    // --- Queries per day for a date range ---

    @Query("SELECT function('date', q.queriedAt), COUNT(q) " +
            "FROM AIQueryLog q " +
            "WHERE q.queriedAt BETWEEN :from AND :to " +
            "GROUP BY function('date', q.queriedAt) " +
            "ORDER BY function('date', q.queriedAt) ASC")
    List<Object[]> countQueriesPerDay(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    // --- OpenAI-specific count for cost calculation ---

    long countByLlmProvider(String llmProvider);
}