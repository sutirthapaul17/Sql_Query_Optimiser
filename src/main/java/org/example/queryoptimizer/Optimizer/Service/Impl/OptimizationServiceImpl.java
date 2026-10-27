package org.example.queryoptimizer.Optimizer.Service.Impl;


import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationDecision;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationRecommendationDto;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationResponseDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisResponseDto;
import org.example.queryoptimizer.Optimizer.Engine.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Engine.OptimizerEngine;
import org.example.queryoptimizer.Optimizer.Service.OptimizationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OptimizationServiceImpl implements OptimizationService {

    private final OptimizerEngine engine;

    @Override
    public OptimizationResponseDto optimize(QueryAnalysisResponseDto queryAnalysis) {

        OptimizationOutcome outcome = engine.optimize(queryAnalysis.sql());
        List<OptimizationRecommendationDto> recommendations = recommendations(outcome);

        return new OptimizationResponseDto(
                decision(outcome, recommendations),
                summary(outcome),
                queryAnalysis,
                recommendations,
                outcome.evidence());
    }

    /**
     * KEEP_ORIGINAL with nothing to suggest      -> BASELINE_RETAINED
     * KEEP_ORIGINAL with index/setting advice    -> RECOMMENDATIONS_AVAILABLE
     * OPTIMIZED and verified by execution        -> RECOMMENDATIONS_AVAILABLE
     * OPTIMIZED but only checked by EXPLAIN      -> EXPERIMENT_REQUIRED (run and compare before trusting it)
     * FAILED                                     -> INSUFFICIENT_EVIDENCE
     */
    private static OptimizationDecision decision(OptimizationOutcome o, List<OptimizationRecommendationDto> recs) {
        return switch (o.decision()) {
            case FAILED -> OptimizationDecision.INSUFFICIENT_EVIDENCE;
            case KEEP_ORIGINAL -> recs.isEmpty()
                    ? OptimizationDecision.BASELINE_RETAINED
                    : OptimizationDecision.RECOMMENDATIONS_AVAILABLE;
            case OPTIMIZED -> Boolean.TRUE.equals(o.resultsMatch())
                    ? OptimizationDecision.RECOMMENDATIONS_AVAILABLE
                    : OptimizationDecision.EXPERIMENT_REQUIRED;
        };
    }

    private static String summary(OptimizationOutcome o) {
        String head = switch (o.decision()) {
            case KEEP_ORIGINAL -> "The baseline query is kept. ";
            case OPTIMIZED -> "A rewritten query is proposed. ";
            case FAILED -> "The optimizer could not complete. ";
        };
        String warnings = o.warnings().isEmpty() ? "" : " Notes: " + String.join(" | ", o.warnings());
        return head + (o.reasoning() == null ? "" : o.reasoning()) + warnings;
    }

    private static List<OptimizationRecommendationDto> recommendations(OptimizationOutcome o) {
        List<OptimizationRecommendationDto> out = new ArrayList<>();

        if (o.decision() == OptimizationOutcome.Decision.OPTIMIZED) {
            List<String> benefits = new ArrayList<>();
            if (o.baselineCost() != null && o.candidateCost() != null && o.baselineCost() > 0) {
                double pct = (1.0 - o.candidateCost() / o.baselineCost()) * 100.0;
                benefits.add(String.format(Locale.ROOT, "Estimated cost %.1f to %.1f (%.0f%% lower)",
                        o.baselineCost(), o.candidateCost(), pct));
            }
            if (o.baselineMs() != null && o.candidateMs() != null) {
                benefits.add(String.format(Locale.ROOT, "Measured time %.1f ms to %.1f ms", o.baselineMs(), o.candidateMs()));
            }
            boolean verified = Boolean.TRUE.equals(o.resultsMatch());
            out.add(new OptimizationRecommendationDto(
                    "Use the rewritten query",
                    (o.reasoning() == null ? "" : o.reasoning()) + "\n\nOptimized query:\n" + o.finalSql(),
                    o.evidence(),
                    benefits,
                    verified
                            ? List.of("Verified on the current data only; re-check after data or schema changes")
                            : List.of("Result equivalence was not verified by execution; confirm the rows match before using it"),
                    verified ? "Based on the current data and statistics"
                            : "Based on estimated cost only; the query was not executed"));
        }
        for (String idx : o.indexRecommendations()) {
            out.add(new OptimizationRecommendationDto(
                    "Create an index: " + idx,
                    o.reasoning(),
                    o.evidence(),
                    List.of("May replace a sequential scan with an index scan"),
                    List.of("Extra storage and slower writes", "Not applied or tested; check with EXPLAIN after creating it"),
                    "The benefit depends on selectivity and was not measured"));
        }
        for (String setting : o.settingRecommendations()) {
            out.add(new OptimizationRecommendationDto(
                    "Change a setting: " + setting,
                    o.reasoning(),
                    o.evidence(),
                    List.of("May avoid disk spills or change the plan the planner picks"),
                    List.of("Affects the whole session, not just this query", "Not applied or tested"),
                    "The effect was not measured"));
        }
        return out;
    }
}

