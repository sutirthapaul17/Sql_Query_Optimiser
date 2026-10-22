package org.example.queryoptimizer.Service;


import org.example.queryoptimizer.Dto.Optimization.OptimizationRequestDto;
import org.example.queryoptimizer.Dto.Optimization.OptimizationResponseDto;
import org.example.queryoptimizer.Dto.response.QueryAnalysisResponseDto;

public interface OptimizationService {

    OptimizationResponseDto optimize(QueryAnalysisResponseDto queryAnalysis);

}
