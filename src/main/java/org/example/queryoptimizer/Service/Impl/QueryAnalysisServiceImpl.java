package org.example.queryoptimizer.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Adapter.Impl.PostgreSQLAdapter;
import org.example.queryoptimizer.Dto.QueryPlanDto;
import org.example.queryoptimizer.Dto.QueryPlanFindingDto;
import org.example.queryoptimizer.Dto.request.QueryAnalysisRequestDto;
import org.example.queryoptimizer.Dto.response.QueryAnalysisResponseDto;

import org.example.queryoptimizer.Dto.response.SqlAnalysisResult;
import org.example.queryoptimizer.Service.OptimizationService;
import org.example.queryoptimizer.Service.QueryAnalysisService;
import org.example.queryoptimizer.Service.QueryPlanAnalyzer;
import org.example.queryoptimizer.Service.SqlAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryAnalysisServiceImpl
        implements QueryAnalysisService {

    private final SqlAnalysisService sqlAnalysisService;
    private final PostgreSQLAdapter postgreSQLAdapter;
    private final QueryPlanAnalyzer queryPlanAnalyzer;

    private final OptimizationService optimizationService;

    @Override
    @Transactional(readOnly = true)
    public QueryAnalysisResponseDto analyze(
            QueryAnalysisRequestDto request) {


        // 1. Extract and validate the SQL structure.
        SqlAnalysisResult analysis =
                sqlAnalysisService.analyze(request.sql());

        // 2. Obtain the baseline PostgreSQL execution plan.
        QueryPlanDto baselinePlan =
                postgreSQLAdapter.getBaselinePlan(request.sql());

        // 3. Analyze the plan.
        List<QueryPlanFindingDto> findings =
                queryPlanAnalyzer.analyze(baselinePlan);

        // 4. Build the complete query analysis result.
        QueryAnalysisResponseDto queryAnalysis = new QueryAnalysisResponseDto(
                request.sql(),
                analysis.tables(),
                analysis.joins(),
                analysis.filters(),
                analysis.groupByColumns(),
                baselinePlan,
                analysis.warnings(),
                findings
        );

        // 5. Pass the analysis to the optimization intelligence pipeline.
        optimizationService.optimize(queryAnalysis);

        // Temporary: preserve the existing return type.
        // We'll change the response contract to include optimization results.
        return queryAnalysis;
    }
}
