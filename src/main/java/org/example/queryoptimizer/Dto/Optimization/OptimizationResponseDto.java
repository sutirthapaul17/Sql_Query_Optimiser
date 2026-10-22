package org.example.queryoptimizer.Dto.Optimization;

import org.example.queryoptimizer.Dto.response.QueryAnalysisResponseDto;

import java.util.List;

public record OptimizationResponseDto(

        OptimizationDecision decision,

        String summary,

        QueryAnalysisResponseDto queryAnalysis,

        List<OptimizationRecommendationDto> recommendations,

        List<String> evidenceReferences

) {}
