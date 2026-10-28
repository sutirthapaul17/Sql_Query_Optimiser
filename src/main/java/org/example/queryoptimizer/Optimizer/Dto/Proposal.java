package org.example.queryoptimizer.Optimizer.Dto;

import java.util.List;

public record Proposal(
        boolean optimize,
        String reasoning,
        String optimizedSql,
        List<String> indexRecommendations,
        List<String> settingRecommendations,
        List<String> evidence
){}
