package org.example.queryoptimizer.Target.Dto;

public record QueryPlanFindingDto(
        String type,
        String severity,
        String nodeType,
        String relationName,
        String message
) {}
