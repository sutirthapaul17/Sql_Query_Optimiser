package org.example.queryoptimizer.Dto;

public record QueryPlanDto(
        String explainMode,
        Double planningTimeMs,
        Double executionTimeMs,
        QueryPlanNodeDto rootPlan
) {}
