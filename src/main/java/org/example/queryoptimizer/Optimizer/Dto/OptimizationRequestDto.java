package org.example.queryoptimizer.Optimizer.Dto;

import jakarta.validation.constraints.NotBlank;

public record OptimizationRequestDto(

        @NotBlank(message = "SQL query must not be blank")
        String sql

) {}
