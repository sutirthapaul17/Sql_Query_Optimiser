package org.example.queryoptimizer.Optimizer.Engine;

import java.util.List;

public record OptimizationOutcome(
        Decision decision,
        String originalSql,
        String finalSql,
        String reasoning,
        List<String> flags,
        List<String> indexRecommendations,
        List<String> settingRecommendations,
        Double baselineCost,
        Double candidateCost,
        Double baselineMs,
        Double candidateMs,
        Boolean resultsMatch,
        List<String> evidence,
        List<String> warnings) {

    public enum Decision { KEEP_ORIGINAL, OPTIMIZED, FAILED }

    public static OptimizationOutcome failed(String sql, String reason) {
        return new OptimizationOutcome(Decision.FAILED, sql, sql, reason, List.of(), List.of(), List.of(),
                null, null, null, null, null, List.of(), List.of());
    }
}
