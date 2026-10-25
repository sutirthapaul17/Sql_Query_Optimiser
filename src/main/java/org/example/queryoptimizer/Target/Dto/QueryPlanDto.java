package org.example.queryoptimizer.Target.Dto;

public record QueryPlanDto(
        String explainMode,
        Double planningTimeMs,
        Double executionTimeMs,
        QueryPlanNodeDto rootPlan
) {}
