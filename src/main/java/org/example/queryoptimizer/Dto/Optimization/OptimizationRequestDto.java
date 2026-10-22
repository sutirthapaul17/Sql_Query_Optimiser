package org.example.queryoptimizer.Dto.Optimization;

import jakarta.validation.constraints.NotBlank;

public record OptimizationRequestDto(

        @NotBlank(message = "SQL query must not be blank")
        String sql

) {}
