package org.example.queryoptimizer.Target.Dto;

import java.util.List;

public record QueryPlanNodeDto(
        String nodeType,
        String relationName,
        String alias,
        String indexName,
        String joinType,
        String strategy,
        Double startupCost,
        Double totalCost,
        Double planRows,
        Integer planWidth,
        Double actualStartupTimeMs,
        Double actualTotalTimeMs,
        Double actualRows,
        Double actualLoops,
        List<QueryPlanNodeDto> children
) {}
