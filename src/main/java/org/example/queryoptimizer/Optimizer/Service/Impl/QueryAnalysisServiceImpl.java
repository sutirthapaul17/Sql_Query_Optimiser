package org.example.queryoptimizer.Optimizer.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationResponseDto;
import org.example.queryoptimizer.Target.Adapter.Impl.PostgreSQLAdapter;
import org.example.queryoptimizer.Target.Dto.QueryPlanDto;
import org.example.queryoptimizer.Target.Dto.QueryPlanFindingDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisRequestDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisResponseDto;

import org.example.queryoptimizer.Optimizer.Dto.SqlAnalysisResult;
import org.example.queryoptimizer.Optimizer.Service.OptimizationService;
import org.example.queryoptimizer.Optimizer.Service.QueryAnalysisService;
import org.example.queryoptimizer.Target.Analysis.QueryPlanAnalyzer;
import org.example.queryoptimizer.Optimizer.Service.SqlAnalysisService;
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
    public OptimizationResponseDto analyze(
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

        // 5. Capture the optimization result.
        OptimizationResponseDto optimizationResponse =
                optimizationService.optimize(queryAnalysis);

        // 6. Return the actual optimization response.
        return optimizationResponse;
    }
}
