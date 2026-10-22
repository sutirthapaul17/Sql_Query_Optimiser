package org.example.queryoptimizer.Dto.response;

public record ExperimentResponse(
        String experimentId,
        String status,
        String message
) {}
