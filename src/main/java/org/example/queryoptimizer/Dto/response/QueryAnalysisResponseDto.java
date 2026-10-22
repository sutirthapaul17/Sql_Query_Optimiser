package org.example.queryoptimizer.Dto.response;

import org.example.queryoptimizer.Dto.QueryPlanDto;
import org.example.queryoptimizer.Dto.QueryPlanFindingDto;

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