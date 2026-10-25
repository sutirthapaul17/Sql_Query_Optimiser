package org.example.queryoptimizer.Optimizer.Service;


import org.example.queryoptimizer.Optimizer.Dto.OptimizationResponseDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisResponseDto;

public interface OptimizationService {

    OptimizationResponseDto optimize(QueryAnalysisResponseDto queryAnalysis);

}
