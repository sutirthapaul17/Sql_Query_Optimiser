package org.example.queryoptimizer.Optimizer.Service;

import org.example.queryoptimizer.Optimizer.Dto.OptimizationResponseDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisRequestDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisResponseDto;

public interface QueryAnalysisService {

    OptimizationResponseDto analyze(
            QueryAnalysisRequestDto request
    );
}
