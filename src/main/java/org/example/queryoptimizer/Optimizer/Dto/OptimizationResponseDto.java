package org.example.queryoptimizer.Optimizer.Dto;

import java.util.List;

public record OptimizationResponseDto(

        OptimizationDecision decision,

        String summary,

        QueryAnalysisResponseDto queryAnalysis,

        List<OptimizationRecommendationDto> recommendations,

        List<String> evidenceReferences

) {}
