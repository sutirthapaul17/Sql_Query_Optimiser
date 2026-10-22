package org.example.queryoptimizer.Dto;

public record QueryPlanFindingDto(
        String type,
        String severity,
        String nodeType,
        String relationName,
        String message
) {}
