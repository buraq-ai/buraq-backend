package com.buraqai.backend.dto;

import java.time.LocalDate;

public class DailyQueryCountDTO {

    private LocalDate date;
    private Long count;

    // Default constructor (required for JSON deserialization)
    public DailyQueryCountDTO() {}

    public DailyQueryCountDTO(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}