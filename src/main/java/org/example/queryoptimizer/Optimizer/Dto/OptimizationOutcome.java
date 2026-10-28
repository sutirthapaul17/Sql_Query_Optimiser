package org.example.queryoptimizer.Optimizer.Dto;

import java.util.List;
import java.util.Map;

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
        List<String> warnings,
        List<Map<String, Object>> retrievedData,
        Boolean dataTruncated

) {

    public enum Decision { KEEP_ORIGINAL, OPTIMIZED, FAILED }

    public static OptimizationOutcome failed(String sql, String reason) {
        return new OptimizationOutcome(
                Decision.FAILED,
                sql,
                sql,
                reason,
                List.of(),
                List.of(),
                List.of(),
                null,  // baselineCost
                null,  // candidateCost
                null,  // baselineMs
                null,  // candidateMs
                null,  // resultsMatch
                List.of(), // evidence
                List.of(), // warnings
                List.of(), // retrievedData
                false      // dataTruncated
        );
    }
}
