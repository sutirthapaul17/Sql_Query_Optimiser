package org.example.queryoptimizer.Dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryAnalysisRequestDto(
        @NotBlank(message = "SQL query must not be empty")
        @Size(max = 20000, message = "SQL query is too long")
        String sql
) {}
