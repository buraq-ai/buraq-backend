package com.buraqai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponseDTO<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;

}