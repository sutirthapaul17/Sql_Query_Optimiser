package org.example.queryoptimizer.Dto.Optimization;

import java.util.List;

public record OptimizationRecommendationDto(

        String title,
        String rationale,
        List<String> supportingEvidence,
        List<String> expectedBenefits,
        List<String> risks,
        String uncertainty
) {}
