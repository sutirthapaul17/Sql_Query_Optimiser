package org.example.queryoptimizer.Optimizer.Dto;

import org.example.queryoptimizer.Target.Dto.QueryPlanDto;
import org.example.queryoptimizer.Target.Dto.QueryPlanFindingDto;

import java.util.List;

public record QueryAnalysisResponseDto(
        String sql,
        List<String> tables,
        List<String> joins,
        List<String> filters,
        List<String> groupByColumns,
        QueryPlanDto baselinePlan,
        List<String> warnings,
        List<QueryPlanFindingDto> findings
) {}