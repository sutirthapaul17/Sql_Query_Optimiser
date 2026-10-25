package org.example.queryoptimizer.Evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperimentRequest(

        @NotBlank(message = "SQL query cannot be empty")
        @Size(max = 100_000, message = "SQL query exceeds maximum length")
        String sql,

        @Min(value = 1, message = "At least one repetition is required")
        @Max(value = 20, message = "Maximum 20 repetitions allowed")
        int repetitions

) {}
